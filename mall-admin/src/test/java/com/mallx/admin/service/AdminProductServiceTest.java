package com.mallx.admin.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.mallx.admin.service.impl.AdminProductServiceImpl;
import com.mallx.common.exception.BusinessException;
import com.mallx.inventory.entity.InventoryStock;
import com.mallx.inventory.service.InventoryStockService;
import com.mallx.product.service.ProductSkuService;

import org.junit.jupiter.api.Test;

/** 组合层阻止删除已有库存的SKU，保留正常草稿删除能力。 */
class AdminProductServiceTest {
    @Test
    void stockedSkuCannotBeDeletedEvenWhenAvailableIsZero() {
        ProductSkuService skus = mock(ProductSkuService.class);
        InventoryStockService stocks = mock(InventoryStockService.class);
        when(stocks.getById(1L)).thenReturn(new InventoryStock());
        assertThrows(
                BusinessException.class,
                () -> new AdminProductServiceImpl(skus, stocks).deleteSku(1L));
        verify(skus).lockSku(1L);
        verify(skus, never()).deleteSku(anyLong());
    }

    @Test
    void locksBeforeCheckingAndDeletingUnusedDraft() {
        ProductSkuService skus = mock(ProductSkuService.class);
        InventoryStockService stocks = mock(InventoryStockService.class);
        new AdminProductServiceImpl(skus, stocks).deleteSku(1L);
        org.mockito.InOrder order = inOrder(skus, stocks);
        order.verify(skus).lockSku(1L);
        order.verify(stocks).getById(1L);
        order.verify(skus).deleteSku(1L);
    }
}
