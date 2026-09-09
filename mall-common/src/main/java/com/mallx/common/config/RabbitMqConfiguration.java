package com.mallx.common.config;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** RabbitMQ 通用基础设施配置，具体交换机、队列和路由仍由各业务模块声明。 */
@Configuration
public class RabbitMqConfiguration {

    /** 统一使用 JSON 传输业务消息，避免各模块重复声明转换器。 */
    @Bean
    public MessageConverter rabbitJsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
