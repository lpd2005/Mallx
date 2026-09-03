package com.mallx.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;

import com.mallx.admin.dto.AdminPageQuery;
import com.mallx.admin.service.AdminOperationLogService;
import com.mallx.admin.service.AdminQueryService;
import com.mallx.admin.vo.AdminOrderSummaryVO;
import com.mallx.common.context.CurrentUserProvider;
import com.mallx.common.result.PageResult;
import com.mallx.common.result.Result;
import com.mallx.order.service.OrderService;
import com.mallx.order.vo.OrderVO;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

/** 订单履约管理接口，使用后台统一RBAC鉴权。 */
@SaCheckPermission("order:manage")
@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class OrderManagementController {
    private final AdminOperationLogService operationLogService;
    private final AdminQueryService queryService;
    private final OrderService service;
    private final CurrentUserProvider users;

    /** 后台按订单号、用户和状态分页查询。 */
    @GetMapping
    public Result<PageResult<AdminOrderSummaryVO>> page(
            @RequestParam(required = false) Long userId,
            @Valid @ModelAttribute AdminPageQuery query) {
        return Result.success(queryService.orders(userId, query));
    }

    /** 查看订单快照和明细。 */
    @GetMapping("/{orderNo}")
    public Result<OrderVO> get(@PathVariable String orderNo) {
        return Result.success(queryService.order(orderNo));
    }

    @PostMapping("/{orderNo}/prepare-shipment")
    public Result<Void> prepare(@PathVariable String orderNo) {
        operationLogService.execute(
                "order:prepare-shipment",
                orderNo,
                () -> service.prepareShipment(orderNo, users.requireUserId()));
        return Result.success();
    }

    @PostMapping("/{orderNo}/ship")
    public Result<Void> ship(@PathVariable String orderNo) {
        operationLogService.execute(
                "order:ship", orderNo, () -> service.shipOrder(orderNo, users.requireUserId()));
        return Result.success();
    }
}
