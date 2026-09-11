package com.mallx.search.service.impl;

import com.mallx.common.enums.ResultCode;
import com.mallx.common.exception.BusinessException;
import com.mallx.product.message.ProductSearchSyncMessage;
import com.mallx.search.vo.ProductSearchFailureVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** 保存搜索同步死信，并支持管理员按事件重放。 */
@Service
@RequiredArgsConstructor
public class ProductSearchFailureService {
    /** Hash field 为 eventId，value 为“spuId|失败时间”。 */
    private static final String FAILURE_KEY = "mallx:search:sync:failures";
    private static final String SEPARATOR = "|";

    private final StringRedisTemplate redisTemplate;
    private final ProductSearchIndexService indexService;

    /** 死信消费者调用该方法留档，避免失败事件无人处理。 */
    public void record(ProductSearchSyncMessage message) {
        String value = message.getSpuId() + SEPARATOR + LocalDateTime.now();
        redisTemplate.opsForHash().put(FAILURE_KEY, message.getEventId(), value);
    }

    /** 返回全部失败事件并按记录时间倒序排列，供后台运维查看。 */
    public List<ProductSearchFailureVO> list() {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(FAILURE_KEY);
        List<ProductSearchFailureVO> result = new ArrayList<>();
        entries.forEach((eventId, value) -> {
            String[] parts = String.valueOf(value).split("\\|", 2);
            if (parts.length == 2) {
                result.add(new ProductSearchFailureVO(
                        String.valueOf(eventId), Long.valueOf(parts[0]), parts[1]));
            }
        });
        result.sort(Comparator.comparing(ProductSearchFailureVO::getFailedAt).reversed());
        return result;
    }

    /**
     * 重放并不是把旧消息再次发送到 RabbitMQ，而是直接按 SPU ID 读取 MySQL 最新状态同步 ES。
     * 只有同步成功后才删除失败记录；再次失败时记录仍然保留。
     */
    public void replay(String eventId) {
        Object value = redisTemplate.opsForHash().get(FAILURE_KEY, eventId);
        if (value == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "搜索同步失败记录不存在");
        }
        String[] parts = String.valueOf(value).split("\\|", 2); // 0: spuId, 1: failedAt
        indexService.synchronize(Long.valueOf(parts[0]));
        redisTemplate.opsForHash().delete(FAILURE_KEY, eventId);
    }
}
