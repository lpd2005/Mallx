package com.mallx.seckill.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.mallx.common.enums.ResultCode;
import com.mallx.common.exception.BusinessException;
import com.mallx.seckill.dto.SeckillPurchaseRequest;
import com.mallx.seckill.entity.SeckillOrderRequest;
import com.mallx.seckill.enums.SeckillRequestStatus;
import com.mallx.seckill.message.SeckillOrderMessage;
import com.mallx.seckill.publisher.SeckillMessagePublisher;
import com.mallx.seckill.service.SeckillOrderRequestService;
import com.mallx.seckill.service.SeckillPurchaseService;
import com.mallx.seckill.service.SeckillRedisService;
import com.mallx.seckill.vo.SeckillResultVO;
import com.mallx.seckill.vo.SeckillSubmitVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户秒杀入口实现。
 *
 * <p>入口不访问商品和库存数据库；活动校验、限购和资格预扣全部由一段 Lua 原子完成。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillPurchaseServiceImpl implements SeckillPurchaseService {
    private final SeckillRedisService redisService;
    private final SeckillMessagePublisher messagePublisher;
    private final SeckillOrderRequestService orderRequestService;

    /** 预扣成功后先登记投递次数再发送消息，避免消费者快速完成后待处理标记被重新加入。 */
    @Override
    public SeckillSubmitVO submit(Long userId, Long itemId, SeckillPurchaseRequest request) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "当前用户身份无效");
        }
        if (itemId == null || itemId <= 0) {
            throw bad("秒杀商品 ID 不正确");
        }
        String requestNo = IdWorker.getIdStr();
        SeckillOrderMessage command = new SeckillOrderMessage(requestNo, null, itemId, null,
                userId, request.getAddressId(), request.getQuantity(), null);
        long result = redisService.reserve(command);
        if (result != 0) {
            throw reserveException(result);
        }
        SeckillOrderMessage message = redisService.getMessage(requestNo);
        if (message == null) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "秒杀请求状态读取失败");
        }
        try {
            redisService.markDispatched(requestNo); // 先登记投递次数，避免消费者快速完成后待处理标记被重新加入
            messagePublisher.send(message); // 投递消息
        } catch (RuntimeException exception) {
            // 资格已在 Redis 中记录，恢复任务会重新投递；入口仍返回可查询的请求号。
            log.error("秒杀消息首次投递失败，等待恢复任务重试，requestNo={}", requestNo, exception);
        }
        return new SeckillSubmitVO(requestNo, SeckillRequestStatus.PENDING.getValue());
    }

    /** Redis 结果优先；结果过期时回退到数据库，并始终校验请求所有者。 */
    @Override
    public SeckillResultVO getResult(Long userId, String requestNo) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "当前用户身份无效");
        }
        if (requestNo == null || requestNo.isBlank() || requestNo.trim().length() > 64) {
            throw bad("秒杀请求号不正确");
        }
        String normalized = requestNo.trim();
        SeckillOrderMessage message = redisService.getMessage(normalized);
        if (message != null) {
            requireOwner(userId, message.getUserId());
            SeckillResultVO result = redisService.getResult(normalized);
            if (result != null) {
                return result;
            }
        }
        SeckillOrderRequest record = orderRequestService.findByRequestNo(normalized);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "秒杀请求不存在或已过期");
        }
        requireOwner(userId, record.getUserId());
        return SeckillResultVO.builder().requestNo(record.getRequestNo()).status(record.getStatus())
                .orderNo(record.getOrderNo()).failReason(record.getFailReason()).build();
    }

    /** 请求结果只允许原用户查询。 */
    private void requireOwner(Long currentUserId, Long ownerId) {
        if (!currentUserId.equals(ownerId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权查询该秒杀请求");
        }
    }

    /** 把 Lua 稳定返回码转换成对客户端安全的业务异常。 */
    private BusinessException reserveException(long code) {
        return switch ((int) code) {
            case 1 -> new BusinessException(ResultCode.NOT_FOUND, "秒杀商品不存在或未开放");
            case 2 -> bad("秒杀活动尚未开始");
            case 3 -> bad("秒杀活动已经结束");
            case 4 -> bad("秒杀商品已售罄");
            case 5 -> bad("已超过该商品的限购数量");
            case 6 -> bad("秒杀请求号重复，请勿重复提交");
            default -> new BusinessException(ResultCode.INTERNAL_ERROR, "秒杀请求处理失败");
        };
    }

    private BusinessException bad(String message) {
        return new BusinessException(ResultCode.BAD_REQUEST, message);
    }
}
