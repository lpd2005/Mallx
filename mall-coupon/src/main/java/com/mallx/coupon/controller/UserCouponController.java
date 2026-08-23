package com.mallx.coupon.controller;

import com.mallx.common.context.CurrentUserProvider;
import com.mallx.common.result.Result;
import com.mallx.coupon.service.CouponUserService;
import com.mallx.coupon.vo.CouponCalculationVO;
import com.mallx.coupon.vo.CouponUserVO;
import com.mallx.coupon.vo.UsableCouponVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/** 当前登录用户的优惠券查询和试算接口。 */
@RestController
@RequestMapping("/users/me/coupons")
@RequiredArgsConstructor
public class UserCouponController {

    private final CouponUserService couponUserService;
    private final CurrentUserProvider currentUserProvider;

    /** 按状态查询当前用户持有的优惠券，状态为空时查询全部。 */
    @GetMapping
    public Result<List<CouponUserVO>> listCoupons(@RequestParam(required = false) Integer status) {
        return Result.success(couponUserService.listUserCoupons(
                currentUserProvider.requireUserId(), status));
    }

    /** 查询在指定可优惠金额下能够使用的优惠券及预计优惠金额。 */
    @GetMapping("/usable")
    public Result<List<UsableCouponVO>> listUsableCoupons(@RequestParam BigDecimal amount) {
        return Result.success(couponUserService.listUsableCoupons(
                currentUserProvider.requireUserId(), amount));
    }

    /** 对指定用户券进行只读试算，不会锁定或消耗优惠券。 */
    @GetMapping("/{couponUserId}/calculation")
    public Result<CouponCalculationVO> calculateDiscount(@PathVariable Long couponUserId,
                                                          @RequestParam BigDecimal amount) {
        return Result.success(couponUserService.calculateDiscount(
                currentUserProvider.requireUserId(), couponUserId, amount));
    }
}
