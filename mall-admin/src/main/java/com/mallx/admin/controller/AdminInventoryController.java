package com.mallx.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;

import com.mallx.admin.dto.*;
import com.mallx.admin.service.*;
import com.mallx.admin.vo.*;
import com.mallx.common.result.*;
import com.mallx.inventory.service.*;
import com.mallx.inventory.vo.*;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 后台库存只开放入库；锁定、扣减、释放仍由订单驱动，避免人工破坏交易状态。 */
@RestController
@RequestMapping("/admin/inventory")
@RequiredArgsConstructor
@SaCheckPermission("inventory:manage")
public class AdminInventoryController {
    private final InventoryStockService stockService;
    private final InventoryOperationService inventoryOperationService;
    private final AdminQueryService queryService;
    private final AdminOperationLogService operationLogService;

    @GetMapping
    public Result<PageResult<InventoryStockVO>> page(
            @RequestParam(required = false) Long skuId,
            @Valid @ModelAttribute AdminPageQuery query) {
        return Result.success(queryService.stocks(skuId, query));
    }

    /** ADM-IN前缀隔离后台入库与其他业务单号，调用方重试必须保持入库号和数量不变。 */
    @PostMapping("/skus/{skuId}/inbound")
    public Result<InventoryStockVO> inbound(
            @PathVariable Long skuId, @Valid @RequestBody StockInboundRequest request) {
        return Result.success(
                operationLogService.execute(
                        "inventory:inbound",
                        skuId + "/" + request.getInboundNo(),
                        () ->
                                stockService.increaseAvailable(
                                        skuId,
                                        request.getQuantity(),
                                        "ADM-IN-" + request.getInboundNo(),
                                        request.getRemark())));
    }

    @GetMapping("/operations")
    public Result<List<InventoryOperationVO>> operations(@RequestParam String businessNo) {
        return Result.success(inventoryOperationService.listByBusinessNo(businessNo));
    }
}
