package com.mallx.order.controller;

import com.mallx.common.context.CurrentUserProvider;
import com.mallx.common.result.Result;
import com.mallx.order.dto.*;
import com.mallx.order.service.OrderService;
import com.mallx.order.vo.OrderVO;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 当前用户普通订单接口。 */
@RestController
@RequestMapping("/users/me/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService service;
    private final CurrentUserProvider users;

    @PostMapping
    public Result<OrderVO> create(@Valid @RequestBody CreateOrderRequest r) {
        return Result.success(service.createOrder(users.requireUserId(), r));
    }

    @GetMapping
    public Result<List<OrderVO>> list(@RequestParam(required = false) Integer status) {
        return Result.success(service.listOrders(users.requireUserId(), status));
    }

    @GetMapping("/{orderNo}")
    public Result<OrderVO> get(@PathVariable String orderNo) {
        return Result.success(service.getOrder(users.requireUserId(), orderNo));
    }

    @PostMapping("/{orderNo}/cancel")
    public Result<Void> cancel(
            @PathVariable String orderNo, @Valid @RequestBody CancelOrderRequest r) {
        service.cancelOrder(users.requireUserId(), orderNo, r.getReason());
        return Result.success();
    }

    @PostMapping("/{orderNo}/receipt")
    public Result<Void> receive(@PathVariable String orderNo) {
        service.confirmReceipt(users.requireUserId(), orderNo);
        return Result.success();
    }
}
