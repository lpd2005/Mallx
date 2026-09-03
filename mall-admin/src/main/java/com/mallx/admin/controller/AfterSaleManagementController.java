package com.mallx.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;

import com.mallx.admin.service.AdminOperationLogService;
import com.mallx.aftersale.dto.AuditAfterSaleRequest;
import com.mallx.aftersale.dto.ReceiveReturnRequest;
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

/** 管理端必须拥有after-sale:manage权限，单纯登录不能审核或发起退款。 */
@SaCheckPermission("after-sale:manage")
@RestController
@RequestMapping("/admin/after-sales")
@RequiredArgsConstructor
public class AfterSaleManagementController {
    private final AdminOperationLogService operationLogService;
    private final AfterSaleService afterSaleService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public Result<PageResult<AfterSaleVO>> page(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(afterSaleService.pageManagement(status, current, size));
    }

    @GetMapping("/{afterSaleNo}")
    public Result<AfterSaleVO> get(@PathVariable String afterSaleNo) {
        return Result.success(afterSaleService.getManagement(afterSaleNo));
    }

    @PostMapping("/{afterSaleNo}/audit")
    public Result<AfterSaleVO> audit(
            @PathVariable String afterSaleNo, @Valid @RequestBody AuditAfterSaleRequest request) {
        Long operatorId = currentUserProvider.requireUserId();
        return Result.success(
                operationLogService.execute(
                        "after-sale:audit",
                        afterSaleNo,
                        () -> afterSaleService.audit(operatorId, afterSaleNo, request)));
    }

    @PostMapping("/{afterSaleNo}/receive-return")
    public Result<AfterSaleVO> receive(
            @PathVariable String afterSaleNo, @Valid @RequestBody ReceiveReturnRequest request) {
        Long operatorId = currentUserProvider.requireUserId();
        return Result.success(
                operationLogService.execute(
                        "after-sale:receive",
                        afterSaleNo,
                        () -> afterSaleService.receiveReturn(operatorId, afterSaleNo, request)));
    }

    @PostMapping("/{afterSaleNo}/refund")
    public Result<AfterSaleVO> refund(@PathVariable String afterSaleNo) {
        Long operatorId = currentUserProvider.requireUserId();
        return Result.success(
                operationLogService.execute(
                        "after-sale:refund",
                        afterSaleNo,
                        () -> afterSaleService.refund(operatorId, afterSaleNo)));
    }
}
