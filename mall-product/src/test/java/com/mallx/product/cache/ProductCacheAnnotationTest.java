package com.mallx.product.cache;

import com.alicp.jetcache.anno.CacheInvalidate;
import com.alicp.jetcache.anno.CachePenetrationProtect;
import com.alicp.jetcache.anno.CacheRefresh;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.mallx.product.service.impl.ProductBrandServiceImpl;
import com.mallx.product.service.impl.ProductCategoryServiceImpl;
import com.mallx.product.service.impl.ProductSkuServiceImpl;
import com.mallx.product.service.impl.ProductSpuServiceImpl;
import com.mallx.product.dto.BrandRequest;
import com.mallx.product.dto.CategoryRequest;
import com.mallx.product.dto.SkuRequest;
import com.mallx.product.dto.SpuRequest;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProductCacheAnnotationTest {

    @Test
    void ordinaryProductQueriesUsePenetrationProtectionWithoutActiveRefresh() throws Exception {
        Method getSpu = ProductSpuServiceImpl.class.getMethod("getSpu", Long.class);
        Method getSku = ProductSkuServiceImpl.class.getMethod("getSku", Long.class);
        Method listBySpu = ProductSkuServiceImpl.class.getMethod("listBySpu", Long.class);

        assertMultiLevelCache(getSpu);
        assertMultiLevelCache(getSku);
        assertMultiLevelCache(listBySpu);
        assertNull(getSpu.getAnnotation(CacheRefresh.class));
        assertNull(getSku.getAnnotation(CacheRefresh.class));
        assertNull(listBySpu.getAnnotation(CacheRefresh.class));

        assertEquals(600, getSpu.getAnnotation(Cached.class).expire());
        assertEquals(30, getSpu.getAnnotation(Cached.class).localExpire());
        assertEquals(480, getSku.getAnnotation(Cached.class).expire());
        assertEquals(25, getSku.getAnnotation(Cached.class).localExpire());
    }

    @Test
    void aggregateHotQueriesRetainActiveRefresh() throws Exception {
        assertRefreshingMultiLevelCache(
                ProductSpuServiceImpl.class.getMethod(
                        "pageProducts", com.mallx.product.dto.ProductPageQuery.class));
        assertRefreshingMultiLevelCache(ProductCategoryServiceImpl.class.getMethod("categoryTree"));
        assertRefreshingMultiLevelCache(ProductBrandServiceImpl.class.getMethod("listEnabledBrands"));
    }

    @Test
    void businessWritesAndDynamicInvalidatorUseJetCacheAnnotations() throws Exception {
        assertInvalidatesCaches(
                ProductCategoryServiceImpl.class.getMethod(
                        "createCategory", CategoryRequest.class));
        assertInvalidatesCaches(
                ProductBrandServiceImpl.class.getMethod("updateBrand", Long.class, BrandRequest.class));
        assertInvalidatesCaches(
                ProductSpuServiceImpl.class.getMethod("updateSpu", Long.class, SpuRequest.class));
        assertInvalidatesCaches(
                ProductSkuServiceImpl.class.getMethod("updateSku", Long.class, SkuRequest.class));
        assertInvalidatesCaches(
                ProductCacheInvalidator.class.getMethod(
                        "invalidateSkuByIds", java.util.Collection.class));
        assertInvalidatesCaches(
                ProductCacheInvalidator.class.getMethod(
                        "invalidateSpuAndSkuListBySpuIds", java.util.Collection.class));
    }

    private void assertMultiLevelCache(Method method) {
        Cached cached = method.getAnnotation(Cached.class);
        assertNotNull(cached, method + " must use @Cached");
        assertEquals(CacheType.BOTH, cached.cacheType());
        assertNotNull(
                method.getAnnotation(CachePenetrationProtect.class),
                method + " must use @CachePenetrationProtect");
    }

    private void assertRefreshingMultiLevelCache(Method method) {
        assertMultiLevelCache(method);
        assertNotNull(method.getAnnotation(CacheRefresh.class), method + " must use @CacheRefresh");
    }

    private void assertInvalidatesCaches(Method method) {
        CacheInvalidate[] annotations = method.getAnnotationsByType(CacheInvalidate.class);
        org.junit.jupiter.api.Assertions.assertTrue(
                annotations.length >= 1, method + " must invalidate related caches");
    }
}
