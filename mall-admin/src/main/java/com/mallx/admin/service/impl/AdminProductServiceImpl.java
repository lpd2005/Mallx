package com.mallx.admin.service.impl;

import com.mallx.admin.service.AdminProductService;
import com.mallx.common.enums.ResultCode;
import com.mallx.common.exception.BusinessException;
import com.mallx.inventory.service.InventoryStockService;
import com.mallx.product.service.ProductSkuService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** 已建立库存记录的SKU只允许下架；删除与后台入库使用同一SKU行锁。 */
@Service
@RequiredArgsConstructor
public class AdminProductServiceImpl implements AdminProductService {
    private final ProductSkuService skuService;
    private final InventoryStockService stockService;

    /**
     * 删除SKU，仅允许删除未建立库存记录的SKU。
     */
    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void deleteSku(Long skuId) {
        skuService.lockSku(skuId);
        if (stockService.getById(skuId) != null)
            throw new BusinessException(ResultCode.BAD_REQUEST, "SKU已有库存记录，请使用下架操作");
        skuService.deleteSku(skuId);
    }
}
