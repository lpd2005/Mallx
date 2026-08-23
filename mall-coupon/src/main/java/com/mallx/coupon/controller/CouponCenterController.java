package com.mallx.coupon.controller;

import com.mallx.common.context.CurrentUserProvider;
import com.mallx.common.result.Result;
import com.mallx.coupon.service.CouponTemplateService;
import com.mallx.coupon.service.CouponUserService;
import com.mallx.coupon.vo.CouponTemplateVO;
import com.mallx.coupon.vo.CouponUserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 前台优惠券中心接口，只负责可领取列表和领取操作。 */
@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
public class CouponCenterController {

    private final CouponTemplateService couponTemplateService;
    private final CouponUserService couponUserService;
    private final CurrentUserProvider currentUserProvider;

    /** 查询当前处于领取期且仍有库存的优惠券。 */
    @GetMapping
    public Result<List<CouponTemplateVO>> listClaimableCoupons() {
        return Result.success(couponTemplateService.listClaimableTemplates());
    }

    /** 当前登录用户领取指定模板的优惠券。 */
    @PostMapping("/{templateId}/claim")
    public Result<CouponUserVO> claimCoupon(@PathVariable Long templateId) {
        return Result.success(couponUserService.claimCoupon(
                currentUserProvider.requireUserId(), templateId));
    }
}
