package com.mallx.order.controller;

import com.mallx.common.context.CurrentUserProvider;
import com.mallx.common.result.Result;
import com.mallx.order.service.OrderService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

/** 订单履约管理接口，具体RBAC权限后续由mall-admin收紧。 */
@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class OrderManagementController {
    private final OrderService service;
    private final CurrentUserProvider users;

    @PostMapping("/{orderNo}/prepare-shipment")
    public Result<Void> prepare(@PathVariable String orderNo) {
        service.prepareShipment(orderNo, users.requireUserId());
        return Result.success();
    }

    @PostMapping("/{orderNo}/ship")
    public Result<Void> ship(@PathVariable String orderNo) {
        service.shipOrder(orderNo, users.requireUserId());
        return Result.success();
    }
}
