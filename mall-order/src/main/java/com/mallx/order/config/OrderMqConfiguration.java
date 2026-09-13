package com.mallx.order.config;

import com.mallx.order.constant.OrderMqNames;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 订单延迟关单拓扑：消息TTL到期后通过DLX转入超时处理队列。 */
@Configuration
public class OrderMqConfiguration {

    @Bean
    public DirectExchange orderDelayExchange() {
        return new DirectExchange(OrderMqNames.DELAY_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange orderTimeoutExchange() {
        return new DirectExchange(OrderMqNames.TIMEOUT_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange orderTimeoutDeadLetterExchange() {
        return new DirectExchange(OrderMqNames.TIMEOUT_DLX, true, false);
    }

    /** 延迟时间写在每条消息的expiration属性中，队列只负责到期后的死信转发。 */
    @Bean
    public Queue orderDelayQueue() {
        return QueueBuilder.durable(OrderMqNames.DELAY_QUEUE)
                .deadLetterExchange(OrderMqNames.TIMEOUT_EXCHANGE)
                .deadLetterRoutingKey(OrderMqNames.TIMEOUT_ROUTING_KEY)
                .build();
    }

    /** 业务消费失败并超过Spring AMQP重试次数后进入独立DLQ。 */
    @Bean
    public Queue orderTimeoutQueue() {
        return QueueBuilder.durable(OrderMqNames.TIMEOUT_QUEUE)
                .deadLetterExchange(OrderMqNames.TIMEOUT_DLX)
                .deadLetterRoutingKey(OrderMqNames.TIMEOUT_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue orderTimeoutDeadLetterQueue() {
        return QueueBuilder.durable(OrderMqNames.TIMEOUT_DLQ).build();
    }

    @Bean
    public Binding orderDelayBinding() {
        return BindingBuilder.bind(orderDelayQueue())
                .to(orderDelayExchange())
                .with(OrderMqNames.DELAY_ROUTING_KEY);
    }

    @Bean
    public Binding orderTimeoutBinding() {
        return BindingBuilder.bind(orderTimeoutQueue())
                .to(orderTimeoutExchange())
                .with(OrderMqNames.TIMEOUT_ROUTING_KEY);
    }

    @Bean
    public Binding orderTimeoutDeadLetterBinding() {
        return BindingBuilder.bind(orderTimeoutDeadLetterQueue())
                .to(orderTimeoutDeadLetterExchange())
                .with(OrderMqNames.TIMEOUT_DLQ_ROUTING_KEY);
    }
}
