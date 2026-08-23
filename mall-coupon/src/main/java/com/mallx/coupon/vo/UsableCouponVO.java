package com.mallx.coupon.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** 在指定结算金额下可使用的用户优惠券。 */
@Data
@Builder
public class UsableCouponVO {
    private CouponUserVO coupon;
    private BigDecimal discountAmount;
    private BigDecimal payableAmount;
}
