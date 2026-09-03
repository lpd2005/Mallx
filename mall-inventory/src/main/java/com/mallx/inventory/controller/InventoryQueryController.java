package com.mallx.inventory.controller;

import com.mallx.common.result.Result;
import com.mallx.inventory.service.InventoryStockService;
import com.mallx.inventory.vo.InventoryStockVO;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 库存只读接口；库存写操作由订单、后台入库或秒杀模块调用Service。 */
@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryQueryController {
    private final InventoryStockService stockService;

    /** 查询指定SKU库存。 */
    @GetMapping("/skus/{skuId}")
    public Result<InventoryStockVO> stock(@PathVariable Long skuId) {
        return Result.success(stockService.getStock(skuId));
    }
}
