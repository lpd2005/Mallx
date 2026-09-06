package com.mallx.common.config;

import com.mallx.common.cache.RedissonProperties;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.util.StringUtils;

/**
 * Redisson 旁路客户端，仅用于分布式锁和概率型数据结构。
 * Spring Data Redis 仍服务于购物车、Sa-Token 和 JetCache，避免替换既有连接工厂。
 */
@Configuration
public class RedissonConfiguration {

    @Bean(destroyMethod = "shutdown")
    @Lazy
    public RedissonClient redissonClient(RedissonProperties properties) {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + properties.getHost() + ":" + properties.getPort())
                .setDatabase(properties.getDatabase())
                .setConnectionPoolSize(properties.getConnectionPoolSize())
                .setConnectionMinimumIdleSize(properties.getConnectionMinimumIdleSize());
        if (StringUtils.hasText(properties.getPassword())) {
            config.setPassword(properties.getPassword());
        }
        return Redisson.create(config);
    }
}
