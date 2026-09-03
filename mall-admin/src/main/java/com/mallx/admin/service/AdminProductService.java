package com.mallx.admin.service;

/** 商品与库存的管理视角组合规则，商品资料规则仍属于mall-product。 */
public interface AdminProductService {
    void deleteSku(Long skuId);
}
