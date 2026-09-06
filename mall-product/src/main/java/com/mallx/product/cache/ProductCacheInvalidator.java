package com.mallx.product.cache;

import com.alicp.jetcache.anno.CacheInvalidate;

import org.springframework.stereotype.Component;

import java.util.Collection;

/** 只处理业务方法参数无法直接表达的动态关联缓存 Key。 */
@Component
public class ProductCacheInvalidator {

    @CacheInvalidate(name = ProductCacheDefinition.Name.SKU_BY_ID, key = ProductCacheDefinition.Key.SKU_IDS, multi = true)
    public void invalidateSkuByIds(Collection<Long> skuIds) {}

    @CacheInvalidate(name = ProductCacheDefinition.Name.SPU_BY_ID, key = ProductCacheDefinition.Key.SPU_IDS, multi = true)
    @CacheInvalidate(name = ProductCacheDefinition.Name.SKUS_BY_SPU_ID, key = ProductCacheDefinition.Key.SPU_IDS, multi = true)
    public void invalidateSpuAndSkuListBySpuIds(Collection<Long> spuIds) {}
}
