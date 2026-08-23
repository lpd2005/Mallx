package com.mallx.coupon.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.mallx.coupon.entity.CouponUser;
import com.mallx.coupon.vo.CouponCalculationVO;
import com.mallx.coupon.vo.CouponUserVO;
import com.mallx.coupon.vo.UsableCouponVO;

import java.math.BigDecimal;
import java.util.List;

/** 用户优惠券领取、计算及订单生命周期服务。 */
public interface CouponUserService extends IService<CouponUser> {
    CouponUserVO claimCoupon(Long userId, Long templateId);
    List<CouponUserVO> listUserCoupons(Long userId, Integer status);
    List<UsableCouponVO> listUsableCoupons(Long userId, BigDecimal eligibleAmount);
    CouponCalculationVO calculateDiscount(Long userId, Long couponUserId, BigDecimal eligibleAmount);
    CouponCalculationVO lockForOrder(Long userId, Long couponUserId, String orderNo, BigDecimal eligibleAmount);
    void confirmUsed(Long couponUserId, String orderNo);
    void rollbackForOrder(Long couponUserId, String orderNo);
    int expireUnusedCoupons(int batchSize);
}
