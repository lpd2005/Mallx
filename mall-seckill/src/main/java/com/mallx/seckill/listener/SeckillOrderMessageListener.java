package com.mallx.seckill.listener;

import com.mallx.common.exception.BusinessException;
import com.mallx.common.enums.ResultCode;
import com.mallx.seckill.constant.SeckillMqNames;
import com.mallx.seckill.constant.SeckillRedisKeys;
import com.mallx.seckill.entity.SeckillOrderRequest;
import com.mallx.seckill.enums.SeckillRequestStatus;
import com.mallx.seckill.message.SeckillOrderMessage;
import com.mallx.seckill.service.SeckillOrderRequestService;
import com.mallx.seckill.service.SeckillRedisService;
import com.mallx.seckill.vo.SeckillResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 秒杀订单消费者。
 *
 * <p>请求级 Redisson 锁只用于串行化同一请求的重复消息，不参与高并发抢购入口，
 * 因此不会把所有秒杀请求退化成串行执行。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillOrderMessageListener {
    private final SeckillOrderRequestService orderRequestService;
    private final SeckillRedisService redisService;
    private final RedissonClient redissonClient;

    /**
     * 消费正常队列并创建正式订单。
     *
     * <p>确定性业务异常可以安全补偿；内部系统异常重新抛出，由 RabbitMQ 重试。</p>
     */
    @RabbitListener(queues = SeckillMqNames.ORDER_QUEUE)
    public void createOrder(SeckillOrderMessage message) {
        RLock lock = redissonClient.getLock(SeckillRedisKeys.requestLock(message.getRequestNo()));
        lock.lock();
        try {
            SeckillResultVO current = redisService.getResult(message.getRequestNo());
            if (current == null) {
                throw new BusinessException(ResultCode.INTERNAL_ERROR, "秒杀请求状态不存在");
            }
            if (SeckillRequestStatus.FAILED.matches(current.getStatus())) {
                orderRequestService.markFailed(message, current.getFailReason());
                return;
            }
            if (SeckillRequestStatus.SUCCESS.matches(current.getStatus())) {
                return;
            }
            String orderNo = orderRequestService.createOrder(message);
            redisService.complete(message.getRequestNo(), message.getItemId(), orderNo);
        } catch (BusinessException exception) {
            if (ResultCode.INTERNAL_ERROR == exception.getResultCode()) {
                throw exception;
            }
            // 商品下架、地址失效等确定性业务失败不再重试，原子归还资格库存。
            redisService.compensate(message, exception.getMessage());
            orderRequestService.markFailed(message, exception.getMessage());
            log.warn("秒杀异步下单业务失败，requestNo={}, reason={}",
                    message.getRequestNo(), exception.getMessage());
        } finally {
            lock.unlock();
        }
    }

    /**
     * 处理达到 RabbitMQ 重试上限的消息。
     *
     * <p>未知系统异常下不能确认订单一定未创建，因此保留 Redis 待处理资格，交给恢复任务重投。</p>
     */
    @RabbitListener(queues = SeckillMqNames.ORDER_DLQ)
    public void handleDeadLetter(SeckillOrderMessage message) {
        RLock lock = redissonClient.getLock(SeckillRedisKeys.requestLock(message.getRequestNo()));
        lock.lock();
        try {
            SeckillOrderRequest record = orderRequestService.findByRequestNo(message.getRequestNo());
            if (record != null && SeckillRequestStatus.SUCCESS.matches(record.getStatus())) {
                redisService.complete(message.getRequestNo(), message.getItemId(), record.getOrderNo());
                return;
            }
            // 系统异常无法证明订单一定未创建，不能冒险归还资格库存；恢复任务会继续重投。
            log.error("秒杀消息进入死信队列，保留待处理资格并等待恢复任务，requestNo={}", message.getRequestNo());
        } finally {
            lock.unlock();
        }
    }
}
