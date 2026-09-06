package com.mallx.product.cache;

import com.mallx.common.cache.CacheProperties;

import com.mallx.product.service.ProductSkuService;
import com.mallx.product.service.ProductSpuService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 直接暴露 Redisson 已实现的 SPU/SKU 布隆过滤器。 */
@Configuration
public class ProductBloomFilterConfiguration {

    @Bean
    public RBloomFilter<Long> existingSpuIdBloomFilter(RedissonClient redissonClient, CacheProperties properties) {
        return createBloomFilter(redissonClient, properties, ProductCacheDefinition.Bloom.EXISTING_SPU_IDS);
    }

    @Bean
    public RBloomFilter<Long> existingSkuIdBloomFilter(RedissonClient redissonClient, CacheProperties properties) {
        return createBloomFilter(redissonClient, properties, ProductCacheDefinition.Bloom.EXISTING_SKU_IDS);
    }

    private RBloomFilter<Long> createBloomFilter(RedissonClient redissonClient, CacheProperties properties, String redisKey) {
        RBloomFilter<Long> filter = redissonClient.getBloomFilter(redisKey);
        filter.tryInit(
                properties.getBloom().getExpectedInsertions(),
                properties.getBloom().getFalseProbability()
        );
        return filter;
    }
}
