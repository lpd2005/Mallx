package com.mallx.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 统一 Redis 键与值的序列化方式。
 */
@Configuration
public class RedisConfiguration {

    /**
     * 创建通用 Redis 模板。
     *
     * <p>键固定使用字符串，值使用 JSON，避免默认 JDK 序列化产生不可读的二进制数据。</p>
     *
     * @param connectionFactory Spring Boot 自动配置的 Redis 连接工厂
     * @return 可由各业务模块注入的 Redis 模板
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer();
        // Hash 与普通 KV 使用相同规则，避免同一业务出现两套序列化格式。
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);
        template.afterPropertiesSet();
        return template;
    }
}
