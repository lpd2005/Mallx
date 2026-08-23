package com.mallx.coupon.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 当前用户持有的优惠券及对应模板规则。 */
@Data
@Builder
public class CouponUserVO {
    private Long id;
    private Long templateId;
    private String couponName;
    private Integer couponType;
    private BigDecimal thresholdAmount;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;
    private BigDecimal maxDiscountAmount;
    private Integer status;
    private String orderNo;
    private LocalDateTime receivedAt;
    private LocalDateTime validStartAt;
    private LocalDateTime validEndAt;
    private LocalDateTime lockedAt;
    private LocalDateTime usedAt;
}
