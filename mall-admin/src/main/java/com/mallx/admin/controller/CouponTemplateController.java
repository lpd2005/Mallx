package com.mallx.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;

import com.mallx.admin.service.AdminOperationLogService;
import com.mallx.common.result.Result;
import com.mallx.coupon.dto.CouponTemplateRequest;
import com.mallx.coupon.service.CouponTemplateService;
import com.mallx.coupon.vo.CouponTemplateVO;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 优惠券模板管理接口；由后台统一拦截器校验运营权限。 */
@SaCheckPermission("coupon:manage")
@RestController
@RequestMapping("/admin/coupon-templates")
@RequiredArgsConstructor
public class CouponTemplateController {
    private final AdminOperationLogService operationLogService;

    private final CouponTemplateService couponTemplateService;

    /** 创建优惠券草稿。 */
    @PostMapping
    public Result<CouponTemplateVO> createTemplate(
            @Valid @RequestBody CouponTemplateRequest request) {
        return Result.success(
                operationLogService.execute(
                        "coupon:create",
                        null,
                        () -> couponTemplateService.createTemplate(request)));
    }

    /** 修改尚未发布的优惠券草稿。 */
    @PutMapping("/{templateId}")
    public Result<CouponTemplateVO> updateTemplate(
            @PathVariable Long templateId, @Valid @RequestBody CouponTemplateRequest request) {
        return Result.success(
                operationLogService.execute(
                        "coupon:update",
                        templateId.toString(),
                        () -> couponTemplateService.updateTemplate(templateId, request)));
    }

    /** 发布优惠券模板。 */
    @PostMapping("/{templateId}/publish")
    public Result<CouponTemplateVO> publishTemplate(@PathVariable Long templateId) {
        return Result.success(
                operationLogService.execute(
                        "coupon:publish",
                        templateId.toString(),
                        () -> couponTemplateService.publishTemplate(templateId)));
    }

    /** 停止继续发放，不影响已经被用户领取的优惠券。 */
    @PostMapping("/{templateId}/stop")
    public Result<CouponTemplateVO> stopTemplate(@PathVariable Long templateId) {
        return Result.success(
                operationLogService.execute(
                        "coupon:stop",
                        templateId.toString(),
                        () -> couponTemplateService.stopTemplate(templateId)));
    }

    /** 按状态查询优惠券模板。 */
    @GetMapping
    public Result<List<CouponTemplateVO>> listTemplates(
            @RequestParam(required = false) Integer status) {
        return Result.success(couponTemplateService.listTemplates(status));
    }
}
