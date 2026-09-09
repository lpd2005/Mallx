package com.mallx.seckill.service.impl;

import com.mallx.seckill.config.SeckillProperties;
import com.mallx.seckill.constant.SeckillRedisKeys;
import com.mallx.seckill.entity.SeckillActivity;
import com.mallx.seckill.entity.SeckillItem;
import com.mallx.seckill.enums.SeckillRequestStatus;
import com.mallx.seckill.message.SeckillOrderMessage;
import com.mallx.seckill.service.SeckillRedisService;
import com.mallx.seckill.vo.SeckillResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀 Redis 操作实现。
 *
 * <p>Java 负责组织 Key 和参数，竞争性读写交给 classpath 下的 Lua 脚本一次完成，
 * 避免“先查询库存、再扣库存”产生并发窗口。</p>
 */
@Service
@RequiredArgsConstructor
public class SeckillRedisServiceImpl implements SeckillRedisService {
    /** 原子完成活动校验、限购、资格库存扣减和请求登记。 */
    private static final DefaultRedisScript<Long> RESERVE_SCRIPT = script("lua/seckill_reserve.lua");
    /** 原子把请求标记为成功并移出待处理集合。 */
    private static final DefaultRedisScript<Long> COMPLETE_SCRIPT = script("lua/seckill_complete.lua");
    /** 原子补回资格库存、用户限购计数并把请求标记为失败。 */
    private static final DefaultRedisScript<Long> COMPENSATE_SCRIPT = script("lua/seckill_compensate.lua");

    private final StringRedisTemplate redisTemplate;
    private final SeckillProperties properties;

    /**
     * 将每个活动商品预热为元数据 Hash 和资格库存 String。
     *
     * <p>库存 Key 已存在时只续期而不重置；Key 丢失时按总库存减已成功销量恢复。</p>
     */
    @Override
    public void warmUp(SeckillActivity activity, List<SeckillItem> items) {
        long ttlSeconds = calculateTtlSeconds(activity.getEndAt());
        for (SeckillItem item : items) {
            String itemKey = SeckillRedisKeys.item(item.getId());
            String stockKey = SeckillRedisKeys.stock(item.getId());
            Map<String, String> metadata = Map.of(
                    "activityId", item.getActivityId().toString(),
                    "skuId", item.getSkuId().toString(),
                    "startAt", toEpochMillis(activity.getStartAt()).toString(),
                    "endAt", toEpochMillis(activity.getEndAt()).toString(),
                    "status", item.getStatus().toString(),
                    "limitPerUser", item.getLimitPerUser().toString(),
                    "seckillPrice", item.getSeckillPrice().toPlainString());
            redisTemplate.opsForHash().putAll(itemKey, metadata);
            redisTemplate.expire(itemKey, ttlSeconds, TimeUnit.SECONDS);
            int remainingStock = Math.max(0, item.getTotalStock() - item.getSoldStock());
            Boolean created = redisTemplate.opsForValue().setIfAbsent(
                    stockKey, String.valueOf(remainingStock), ttlSeconds, TimeUnit.SECONDS);
            if (Boolean.FALSE.equals(created)) {
                redisTemplate.expire(stockKey, ttlSeconds, TimeUnit.SECONDS);
            }
            // 已成功购买的用户 ID 集合，用于限购。
            redisTemplate.expire(SeckillRedisKeys.purchased(item.getId()), ttlSeconds, TimeUnit.SECONDS);
        }
    }

    /** 关闭活动时只修改元数据状态，使 Lua 拒绝后续请求，保留历史状态供排查。 */
    @Override
    public void disableItems(List<SeckillItem> items) {
        for (SeckillItem item : items) {
            String itemKey = SeckillRedisKeys.item(item.getId());
            // 草稿活动或已经自然过期的活动可能没有元数据 Key，不能为其制造无 TTL 的残留 Hash。
            if (Boolean.TRUE.equals(redisTemplate.hasKey(itemKey))) {
                redisTemplate.opsForHash().put(itemKey, "status", "0");
            }
        }
    }

    /**
     * 调用资格预扣脚本。
     *
     * <p>KEYS 顺序必须与 {@code seckill_reserve.lua} 文件头一致；ARGV 使用字符串传递，
     * Lua 在需要数值计算的位置显式调用 tonumber。</p>
     */

    /*
    先检查状态，商品是否存在，活动时间的判断，是否卖完，是否超限购，请求是否重复
    1.扣减库存
    2.用户购买计数，并设置ttl
    3.修改请求信息request （重） 并设置ttl
    4.将请求request加入请求队列ZADD ,设置时间戳
    5.当前秒杀商品待处理pending加一，并设置ttl
     */
    @Override
    public long reserve(SeckillOrderMessage message) {
        List<String> keys = List.of(
                SeckillRedisKeys.item(message.getItemId()),
                SeckillRedisKeys.stock(message.getItemId()),
                SeckillRedisKeys.purchased(message.getItemId()),
                SeckillRedisKeys.request(message.getRequestNo()),
                SeckillRedisKeys.PENDING_REQUESTS,
                SeckillRedisKeys.pendingCount(message.getItemId()));
        Long result = redisTemplate.execute(RESERVE_SCRIPT, keys,
                message.getUserId().toString(), message.getQuantity().toString(),
                message.getAddressId().toString(), message.getItemId().toString(),
                String.valueOf(System.currentTimeMillis()),
                String.valueOf(TimeUnit.SECONDS.toMillis(properties.getRequestTtlSeconds())),
                message.getRequestNo());
        return result == null ? -1L : result;
    }

    /** 读取 Lua 保存的请求 Hash，并还原出可重复投递的完整消息。 */
    @Override
    public SeckillOrderMessage getMessage(String requestNo) {
        Map<Object, Object> values = redisTemplate.opsForHash().entries(SeckillRedisKeys.request(requestNo));
        if (values.isEmpty()) {
            return null;
        }
        return new SeckillOrderMessage(requestNo,
                longValue(values, "activityId"), longValue(values, "itemId"),
                longValue(values, "skuId"), longValue(values, "userId"),
                longValue(values, "addressId"), intValue(values, "quantity"),
                decimalValue(values, "seckillPrice"));
    }

    /** 订单创建成功后原子更新请求结果，重复执行不会改变终态。 */
    /*
    1.判断状态，幂等
    2.对于request中的状态改为1（成功），添加字段orderNo
    3.将request从PENDING_REQUESTS队列移除
    4.当前秒杀商品待处理pending减一
    5.判断pending，为0则删除
     */

    @Override
    public void complete(String requestNo, Long itemId, String orderNo) {
        redisTemplate.execute(COMPLETE_SCRIPT,
                List.of(SeckillRedisKeys.request(requestNo), SeckillRedisKeys.PENDING_REQUESTS, SeckillRedisKeys.pendingCount(itemId)),
                orderNo,
                requestNo);
    }

    /** 仅处理中请求可以执行补偿，因此消费重试不会重复增加库存。 */
    /*
    1.判断状态，幂等
    2.库存回复
    3.用户购买计数减相对应的库存，小于等于0后删除purchased
    4.设置request的状态为2（失败），添加字段failReason失败原因
    5.将requestNo从PENDING_REQUESTS队列移除
    6.当前秒杀商品待处理pending减一
    7.判断pending，为0则删除
     */
    @Override
    public void compensate(SeckillOrderMessage message, String reason) {
        redisTemplate.execute(COMPENSATE_SCRIPT,
                List.of(SeckillRedisKeys.request(message.getRequestNo()),
                        SeckillRedisKeys.stock(message.getItemId()),
                        SeckillRedisKeys.purchased(message.getItemId()),
                        SeckillRedisKeys.PENDING_REQUESTS,
                        SeckillRedisKeys.pendingCount(message.getItemId())),
                message.getQuantity().toString(),
                message.getUserId().toString(),
                normalizeReason(reason),
                message.getRequestNo());
    }

    /** 查询客户端轮询所需的最小结果字段。 */
    @Override
    public SeckillResultVO getResult(String requestNo) {
        Map<Object, Object> values = redisTemplate.opsForHash()
                .entries(SeckillRedisKeys.request(requestNo));
        if (values.isEmpty()) {
            return null;
        }
        return SeckillResultVO.builder()
                .requestNo(requestNo)
                .status(intValue(values, "status"))
                .orderNo(stringValue(values, "orderNo"))
                .failReason(stringValue(values, "failReason"))
                .build();
    }

    /** 计数器只在资格预扣成功后创建，缺失或非正数都表示没有待处理请求。 */
    @Override
    public boolean hasPendingRequests(Long itemId) {
        String value = redisTemplate.opsForValue().get(SeckillRedisKeys.pendingCount(itemId));
        return value != null && Long.parseLong(value) > 0;
    }

    /**
     * 从待处理 ZSet 中读取达到重投时间的请求。
     *
     * <p>已完成、已失败或请求 Hash 已过期的成员会被惰性清理。</p>
     */
    @Override
    public List<SeckillOrderMessage> findRetryableMessages() {
        long deadline = System.currentTimeMillis()
                - TimeUnit.SECONDS.toMillis(properties.getPendingRetryDelaySeconds());
        Set<String> requestNos = redisTemplate.opsForZSet().rangeByScore(
                SeckillRedisKeys.PENDING_REQUESTS, 0, deadline,
                0, properties.getPendingBatchSize());
        if (requestNos == null || requestNos.isEmpty()) {
            return List.of();
        }
        List<SeckillOrderMessage> result = new ArrayList<>();
        for (String requestNo : requestNos) {
            String requestKey = SeckillRedisKeys.request(requestNo);
            String status = stringValue(redisTemplate.opsForHash().entries(requestKey), "status");
            if (status == null || !String.valueOf(SeckillRequestStatus.PENDING.getValue()).equals(status)) {
                redisTemplate.opsForZSet().remove(SeckillRedisKeys.PENDING_REQUESTS, requestNo);
                continue;
            }
            SeckillOrderMessage message = getMessage(requestNo);
            if (message == null) {
                redisTemplate.opsForZSet().remove(SeckillRedisKeys.PENDING_REQUESTS, requestNo);
            } else {
                result.add(message);
            }
        }
        return result;
    }

    /** 增加可观测的投递次数，并将 ZSet 分值更新为本次投递时间。 */
    @Override
    public void markDispatched(String requestNo) {
        String requestKey = SeckillRedisKeys.request(requestNo);
        redisTemplate.opsForHash().increment(requestKey, "dispatchAttempts", 1L);
        redisTemplate.opsForZSet().add(
                SeckillRedisKeys.PENDING_REQUESTS, requestNo, System.currentTimeMillis());
    }

    /** Redis 数据至少保留到活动结束后一个完整的请求结果周期。 */
    private long calculateTtlSeconds(LocalDateTime endAt) {
        long untilEnd = Math.max(0L, Duration.between(LocalDateTime.now(), endAt).getSeconds());
        return Math.max(60L, untilEnd + properties.getRequestTtlSeconds());
    }

    /** 使用服务器所在时区把活动时间转换成 Lua 比较使用的毫秒时间戳。 */
    private Long toEpochMillis(LocalDateTime value) {
        return value.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /** 从 Redis Hash 中读取 Long 字段。 */
    private Long longValue(Map<Object, Object> values, String key) {
        String value = stringValue(values, key);
        return value == null ? null : Long.valueOf(value);
    }

    /** 从 Redis Hash 中读取 Integer 字段。 */
    private Integer intValue(Map<Object, Object> values, String key) {
        String value = stringValue(values, key);
        return value == null ? null : Integer.valueOf(value);
    }

    /** 从 Redis Hash 中读取价格字段，避免使用浮点数丢失精度。 */
    private BigDecimal decimalValue(Map<Object, Object> values, String key) {
        String value = stringValue(values, key);
        return value == null ? null : new BigDecimal(value);
    }

    /** 兼容 StringRedisTemplate 返回的 Object 视图并统一转换为字符串。 */
    private String stringValue(Map<Object, Object> values, String key) {
        Object value = values.get(key);
        return value == null ? null : Objects.toString(value);
    }

    /** Redis 请求结果只保存简短业务原因，不保存异常堆栈。 */
    private String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "秒杀下单失败";
        }
        String value = reason.trim();
        return value.length() > 200 ? value.substring(0, 200) : value;
    }

    /** 按 mall-cart 相同方式从 classpath 创建可复用的 Redis 脚本对象。 */
    private static DefaultRedisScript<Long> script(String path) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(path));
        script.setResultType(Long.class);
        return script;
    }
}
