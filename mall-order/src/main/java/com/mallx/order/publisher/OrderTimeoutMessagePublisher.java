package com.mallx.order.publisher;

import com.mallx.order.constant.OrderMqNames;
import com.mallx.order.message.OrderTimeoutMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/** 在订单事务提交后发布延迟消息，并记录Broker确认和不可路由结果。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutMessagePublisher {
    private final RabbitTemplate rabbitTemplate;

    /**
     * 事务存在时注册afterCommit回调；非事务调用则立即发送。
     * 这样不会出现订单回滚、消息却已经进入RabbitMQ的幽灵关单消息。
     */
    public void sendAfterCommit(OrderTimeoutMessage message) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) { // 事务存在时注册afterCommit回调
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendSafely(message);
                }
            });
            return;
        }
        sendSafely(message);
    }

    /** 根据数据库过期时间计算本次消息TTL，最小1毫秒以便迟到消息立即转发。 */
    public void send(OrderTimeoutMessage message) {
        requireValid(message);
        String orderNo = message.getOrderNo().trim();
        long delayMillis = Math.max(1L, Duration.between(LocalDateTime.now(), message.getExpireAt()).toMillis());
        String correlationId = orderNo + ":" + UUID.randomUUID();
        CorrelationData correlationData = new CorrelationData(correlationId);

        rabbitTemplate.convertAndSend(
                OrderMqNames.DELAY_EXCHANGE,
                OrderMqNames.DELAY_ROUTING_KEY,
                message,
                amqpMessage -> {
                    amqpMessage.getMessageProperties().setMessageId(orderNo); // 稳定业务主键
                    amqpMessage.getMessageProperties().setExpiration(String.valueOf(delayMillis)); // 延迟时间
                    amqpMessage.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT); // 持久化
                    return amqpMessage;
                },
                correlationData);

        correlationData.getFuture().whenComplete((confirm, throwable) -> {
            if (throwable != null) {
                log.error("订单超时消息Confirm异常，orderNo={}, correlationId={}",
                        orderNo, correlationId, throwable);
                return;
            }
            if (confirm == null || !confirm.isAck()) {
                log.error("订单超时消息被RabbitMQ拒绝，orderNo={}, correlationId={}, cause={}",
                        orderNo, correlationId, confirm == null ? null : confirm.getReason());
                return;
            }
            ReturnedMessage returned = correlationData.getReturned();
            if (returned != null) {
                log.error("订单超时消息无法路由，orderNo={}, correlationId={}, exchange={}, "
                                + "routingKey={}, replyCode={}, replyText={}",
                        orderNo, correlationId, returned.getExchange(), returned.getRoutingKey(),
                        returned.getReplyCode(), returned.getReplyText());
                return;
            }
            log.debug("订单超时消息投递成功，orderNo={}, correlationId={}", orderNo, correlationId);
        });
    }

    private void requireValid(OrderTimeoutMessage message) {
        if (message == null || message.getOrderNo() == null || message.getOrderNo().isBlank()
                || message.getExpireAt() == null) {
            throw new IllegalArgumentException("订单超时消息缺少订单号或过期时间");
        }
    }

    /** 订单已经提交后，MQ瞬时异常不能再让接口表现为数据库回滚；兜底任务会最终关单。 */
    private void sendSafely(OrderTimeoutMessage message) {
        try {
            send(message);
        } catch (RuntimeException exception) {
            String orderNo = message == null ? null : message.getOrderNo();
            log.error("订单已提交但超时消息发送失败，等待数据库补偿，orderNo={}", orderNo, exception);
        }
    }
}
