package com.mallx.seckill.publisher;

import com.mallx.seckill.constant.SeckillMqNames;
import com.mallx.seckill.message.SeckillOrderMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** 秒杀订单消息发布器，统一封装交换机、路由键及单次投递确认。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillMessagePublisher {
    private final RabbitTemplate rabbitTemplate;

    /**
     * 将 Redis 资格预扣成功的请求发送到异步下单队列。
     *
     * <p>requestNo 是跨重投保持不变的业务请求号；correlationId 标识某一次 MQ 发布，
     * 每次重投都重新生成。无论发布结果如何，这里都不会删除 Redis 待处理记录。</p>
     */
    public void send(SeckillOrderMessage message) {
        String requestNo = message.getRequestNo();
        String correlationId = requestNo + ":" + UUID.randomUUID();
        CorrelationData correlationData = new CorrelationData(correlationId);

        rabbitTemplate.convertAndSend(
                SeckillMqNames.ORDER_EXCHANGE,
                SeckillMqNames.ORDER_ROUTING_KEY,
                message,
                correlationData);

        correlationData.getFuture().whenComplete((confirm, throwable) -> {
            if (throwable != null) {
                log.error("秒杀消息 Confirm 异常，requestNo={}, correlationId={}", requestNo, correlationId, throwable);
                return;
            }
            if (confirm == null) {
                log.error("秒杀消息 Confirm 结果为空，requestNo={}, correlationId={}", requestNo, correlationId);
                return;
            }
            if (!confirm.isAck()) {
                log.error("秒杀消息被 RabbitMQ 拒绝，requestNo={}, correlationId={}, cause={}", requestNo, correlationId, confirm.getReason());
                return;
            }

            ReturnedMessage returned = correlationData.getReturned();
            if (returned != null) {
                log.error("秒杀消息无法路由，requestNo={}, correlationId={}, exchange={}, "
                                + "routingKey={}, replyCode={}, replyText={}",
                        requestNo, correlationId, returned.getExchange(), returned.getRoutingKey(),
                        returned.getReplyCode(), returned.getReplyText());
                return;
            }

            // 这里只能确认消息进入队列，订单是否创建成功仍以消费者更新的请求状态为准。
            log.debug("秒杀消息投递成功，requestNo={}, correlationId={}", requestNo, correlationId);
        });
    }
}
