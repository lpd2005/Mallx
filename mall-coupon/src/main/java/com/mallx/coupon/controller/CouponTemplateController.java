package com.mallx.coupon.controller;

import com.mallx.common.context.CurrentUserProvider;
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

/** 优惠券模板管理接口；具体运营权限将在mall-admin接入时进一步收紧。 */
@RestController
@RequestMapping("/admin/coupon-templates")
@RequiredArgsConstructor
public class CouponTemplateController {

    private final CouponTemplateService couponTemplateService;
    private final CurrentUserProvider currentUserProvider;

    /** 创建优惠券草稿。 */
    @PostMapping
    public Result<CouponTemplateVO> createTemplate(@Valid @RequestBody CouponTemplateRequest request) {
        requireLogin();
        return Result.success(couponTemplateService.createTemplate(request));
    }

    /** 修改尚未发布的优惠券草稿。 */
    @PutMapping("/{templateId}")
    public Result<CouponTemplateVO> updateTemplate(@PathVariable Long templateId,
                                                    @Valid @RequestBody CouponTemplateRequest request) {
        requireLogin();
        return Result.success(couponTemplateService.updateTemplate(templateId, request));
    }

    /** 发布优惠券模板。 */
    @PostMapping("/{templateId}/publish")
    public Result<CouponTemplateVO> publishTemplate(@PathVariable Long templateId) {
        requireLogin();
        return Result.success(couponTemplateService.publishTemplate(templateId));
    }

    /** 停止继续发放，不影响已经被用户领取的优惠券。 */
    @PostMapping("/{templateId}/stop")
    public Result<CouponTemplateVO> stopTemplate(@PathVariable Long templateId) {
        requireLogin();
        return Result.success(couponTemplateService.stopTemplate(templateId));
    }

    /** 按状态查询优惠券模板。 */
    @GetMapping
    public Result<List<CouponTemplateVO>> listTemplates(@RequestParam(required = false) Integer status) {
        requireLogin();
        return Result.success(couponTemplateService.listTemplates(status));
    }

    /** 当前阶段只验证登录态，RBAC管理权限由后续mall-admin统一接入。 */
    private void requireLogin() {
        currentUserProvider.requireUserId();
    }
}
