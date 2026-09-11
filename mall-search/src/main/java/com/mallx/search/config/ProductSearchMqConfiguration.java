package com.mallx.search.config;

import com.mallx.product.constant.ProductSearchMqNames;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 商品索引同步队列及死信队列。 */
@Configuration
@EnableScheduling
public class ProductSearchMqConfiguration {

    /** 商品变更事件使用持久化直连交换机，路由关系由固定 routing key 控制。 */
    @Bean
    public DirectExchange productSearchExchange() {
        return new DirectExchange(ProductSearchMqNames.EXCHANGE, true, false);
    }

    /** 消费重试耗尽后的消息被转发到该死信交换机。 */
    @Bean
    public DirectExchange productSearchDeadLetterExchange() {
        return new DirectExchange(ProductSearchMqNames.DLX, true, false);
    }

    /**
     * 正常索引同步队列。
     *
     * <p>队列声明了 DLX，监听器异常在框架重试耗尽后会进入死信队列，
     * 不会无限回到业务队列。</p>
     */
    @Bean
    public Queue productSearchQueue() {
        return QueueBuilder.durable(ProductSearchMqNames.QUEUE)
                .deadLetterExchange(ProductSearchMqNames.DLX)
                .deadLetterRoutingKey(ProductSearchMqNames.DLQ_ROUTING_KEY)
                .build();
    }

    /** 保存无法正常完成索引同步的事件，等待留档和人工重放。 */
    @Bean
    public Queue productSearchDeadLetterQueue() {
        return QueueBuilder.durable(ProductSearchMqNames.DLQ).build();
    }

    /** 把商品搜索事件路由到正常同步队列。 */
    @Bean
    public Binding productSearchBinding() {
        return BindingBuilder.bind(productSearchQueue())
                .to(productSearchExchange())
                .with(ProductSearchMqNames.ROUTING_KEY);
    }

    /** 把死信事件路由到独立死信队列。 */
    @Bean
    public Binding productSearchDeadLetterBinding() {
        return BindingBuilder.bind(productSearchDeadLetterQueue())
                .to(productSearchDeadLetterExchange())
                .with(ProductSearchMqNames.DLQ_ROUTING_KEY);
    }
}
