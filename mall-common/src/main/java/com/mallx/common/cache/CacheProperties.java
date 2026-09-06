package com.mallx.common.cache;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Redisson 商品布隆过滤器参数。JetCache 的容量和过期时间由 jetcache 配置负责。 */
@Data
@Component
@ConfigurationProperties(prefix = "mallx.cache")
public class CacheProperties {
    private final Bloom bloom = new Bloom();

    /** 布隆过滤器容量参数。 */
    @Data
    public static class Bloom {
        private long expectedInsertions = 1_000_000L; // 期望插入数量
        private double falseProbability = 0.001D; // 期望误判率
    }
}
