package com.mallx.coupon.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** 优惠券对指定可优惠金额的计算结果。 */
@Data
@Builder
public class CouponCalculationVO {
    private Long couponUserId;
    /** 由订单模块传入的金额，例如会员折扣后的商品金额。 */
    private BigDecimal eligibleAmount; // 可用金额
    private BigDecimal discountAmount; // 优惠金额
    private BigDecimal payableAmount; // 应付金额
}
