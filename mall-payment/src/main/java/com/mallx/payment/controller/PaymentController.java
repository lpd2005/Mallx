package com.mallx.payment.controller;

import com.mallx.common.context.CurrentUserProvider;
import com.mallx.common.result.Result;
import com.mallx.payment.dto.MockPayRequest;
import com.mallx.payment.service.PaymentService;
import com.mallx.payment.vo.PaymentVO;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

/** 当前用户支付接口。 */
@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService service;
    private final CurrentUserProvider users;

    @PostMapping("/orders/{orderNo}/payments")
    public Result<PaymentVO> create(@PathVariable String orderNo) {
        return Result.success(service.createPayment(users.requireUserId(), orderNo));
    }

    @GetMapping("/payments/{paymentNo}")
    public Result<PaymentVO> get(@PathVariable String paymentNo) {
        return Result.success(service.getPayment(users.requireUserId(), paymentNo));
    }

    @PostMapping("/payments/{paymentNo}/mock-pay")
    public Result<PaymentVO> pay(
            @PathVariable String paymentNo, @Valid @RequestBody MockPayRequest r) {
        return Result.success(service.mockPay(users.requireUserId(), paymentNo, r.getRequestNo()));
    }
}
