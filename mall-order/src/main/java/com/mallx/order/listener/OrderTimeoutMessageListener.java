package com.mallx.order.listener;

import com.mallx.order.constant.OrderMqNames;
import com.mallx.order.message.OrderTimeoutMessage;
import com.mallx.order.publisher.OrderTimeoutMessagePublisher;
import com.mallx.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** 消费到期消息；数据库订单状态和expireAt是最终关单依据。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutMessageListener {
    private final OrderService orderService;
    private final OrderTimeoutMessagePublisher timeoutMessagePublisher;

    /**
     * 重复消息、已支付订单和已取消订单都会由服务层幂等跳过。
     * RabbitMQ可能在毫秒边界略早投递，早到消息必须按剩余时间重新延迟，不能直接丢弃。
     */
    @RabbitListener(queues = OrderMqNames.TIMEOUT_QUEUE)
    public void closeExpiredOrder(OrderTimeoutMessage message) {
        if (message == null || message.getOrderNo() == null || message.getOrderNo().isBlank()
                || message.getExpireAt() == null) {
            throw new IllegalArgumentException("订单超时消息缺少订单号或过期时间");
        }
        if (message.getExpireAt().isAfter(LocalDateTime.now())) {
            timeoutMessagePublisher.send(message);
            log.debug("订单超时消息早到，已按剩余时间重新投递，orderNo={}, expireAt={}",
                    message.getOrderNo(), message.getExpireAt());
            return;
        }
        boolean closed = orderService.closeExpiredOrder(message.getOrderNo());
        if (closed) {
            log.info("待支付订单已通过延迟消息关闭，orderNo={}", message.getOrderNo());
        } else {
            log.debug("订单无需超时关闭，orderNo={}", message.getOrderNo());
        }
    }
}
