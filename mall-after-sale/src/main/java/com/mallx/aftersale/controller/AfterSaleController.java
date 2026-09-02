package com.mallx.aftersale.controller;

import com.mallx.aftersale.dto.CreateAfterSaleRequest;
import com.mallx.aftersale.dto.ReturnShipmentRequest;
import com.mallx.aftersale.service.AfterSaleService;
import com.mallx.aftersale.vo.AfterSaleVO;
import com.mallx.common.context.CurrentUserProvider;
import com.mallx.common.result.PageResult;
import com.mallx.common.result.Result;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 当前用户售后接口，不接受前端指定用户ID或退款金额。 */
@RestController
@RequestMapping("/users/me/after-sales")
@RequiredArgsConstructor
public class AfterSaleController {
    private final AfterSaleService afterSaleService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    public Result<AfterSaleVO> apply(@Valid @RequestBody CreateAfterSaleRequest request) {
        return Result.success(afterSaleService.apply(currentUserProvider.requireUserId(), request));
    }

    @GetMapping
    public Result<PageResult<AfterSaleVO>> page(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(
                afterSaleService.pageMine(
                        currentUserProvider.requireUserId(), status, current, size));
    }

    @GetMapping("/{afterSaleNo}")
    public Result<AfterSaleVO> get(@PathVariable String afterSaleNo) {
        return Result.success(
                afterSaleService.getMine(currentUserProvider.requireUserId(), afterSaleNo));
    }

    @PostMapping("/{afterSaleNo}/cancel")
    public Result<AfterSaleVO> cancel(@PathVariable String afterSaleNo) {
        return Result.success(
                afterSaleService.cancel(currentUserProvider.requireUserId(), afterSaleNo));
    }

    @PostMapping("/{afterSaleNo}/return-shipment")
    public Result<AfterSaleVO> submitReturn(
            @PathVariable String afterSaleNo, @Valid @RequestBody ReturnShipmentRequest request) {
        return Result.success(
                afterSaleService.submitReturn(
                        currentUserProvider.requireUserId(), afterSaleNo, request));
    }
}
