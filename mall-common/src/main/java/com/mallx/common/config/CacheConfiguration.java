package com.mallx.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * L1 Caffeine 默认配置；业务模块自行声明缓存名称与失效策略。
 */
@Configuration
@EnableCaching
public class CacheConfiguration {

    @Bean
    /**
     * 配置平台默认的 L1 本地缓存：容量上限 10,000，写入后 5 分钟过期。
     * 业务热点与 TTL 需要根据压测结果在对应模块中进一步细化。
     */
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofMinutes(5)));
        return cacheManager;
    }
}
