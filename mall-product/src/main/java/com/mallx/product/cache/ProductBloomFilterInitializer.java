package com.mallx.product.cache;

import com.mallx.product.entity.ProductSku;
import com.mallx.product.entity.ProductSpu;
import com.mallx.product.service.ProductSkuService;
import com.mallx.product.service.ProductSpuService;
import jakarta.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;

import org.springframework.stereotype.Component;

import java.util.List;

/** 应用启动时把数据库中已有的全部 SPU/SKU ID 写入 Redisson BloomFilter。 */
@Component
@RequiredArgsConstructor
public class ProductBloomFilterInitializer {
    private final RBloomFilter<Long> existingSpuIdBloomFilter;
    private final RBloomFilter<Long> existingSkuIdBloomFilter;
    private final ProductSpuService spuService;
    private final ProductSkuService skuService;


    @PostConstruct // 在 Spring 容器初始化完成后执行
    public void initialize() {
        List<Long> spuIds = spuService.lambdaQuery()
                .select(ProductSpu::getId)
                .list()
                .stream()
                .map(ProductSpu::getId)
                .toList();
        List<Long> skuIds = skuService.lambdaQuery()
                .select(ProductSku::getId)
                .list()
                .stream()
                .map(ProductSku::getId)
                .toList();
        existingSpuIdBloomFilter.add(spuIds);
        existingSkuIdBloomFilter.add(skuIds);
    }

}
