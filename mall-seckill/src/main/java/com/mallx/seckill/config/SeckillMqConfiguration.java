package com.mallx.seckill.config;

import com.mallx.seckill.constant.SeckillMqNames;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 秒杀订单队列及死信队列。 */
@Configuration
@EnableScheduling
public class SeckillMqConfiguration {

    /** 声明持久化业务交换机，应用重启后拓扑仍然存在。 */
    @Bean
    public DirectExchange seckillOrderExchange() {
        return new DirectExchange(SeckillMqNames.ORDER_EXCHANGE, true, false);
    }

    /** 声明接收消费失败消息的持久化死信交换机。 */
    @Bean
    public DirectExchange seckillOrderDeadLetterExchange() {
        return new DirectExchange(SeckillMqNames.ORDER_DLX, true, false);
    }

    /** 业务队列绑定死信交换机，监听重试耗尽后消息会自动转入死信队列。 */
    @Bean
    public Queue seckillOrderQueue() {
        return QueueBuilder.durable(SeckillMqNames.ORDER_QUEUE)
                .deadLetterExchange(SeckillMqNames.ORDER_DLX)
                .deadLetterRoutingKey(SeckillMqNames.ORDER_DLQ_ROUTING_KEY)
                .build();
    }

    /** 声明持久化死信队列。 */
    @Bean
    public Queue seckillOrderDeadLetterQueue() {
        return QueueBuilder.durable(SeckillMqNames.ORDER_DLQ).build();
    }

    /** 使用创建订单路由键绑定业务队列。 */
    @Bean
    public Binding seckillOrderBinding() {
        return BindingBuilder.bind(seckillOrderQueue())
                .to(seckillOrderExchange())
                .with(SeckillMqNames.ORDER_ROUTING_KEY);
    }

    /** 绑定死信队列，用于接收业务队列拒绝的消息。 */
    @Bean
    public Binding seckillOrderDeadLetterBinding() {
        return BindingBuilder.bind(seckillOrderDeadLetterQueue())
                .to(seckillOrderDeadLetterExchange())
                .with(SeckillMqNames.ORDER_DLQ_ROUTING_KEY);
    }
}
