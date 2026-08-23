package com.mallx.coupon.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 优惠券模板展示信息。 */
@Data
@Builder
public class CouponTemplateVO {
    private Long id;
    private String couponName;
    private Integer couponType;
    private BigDecimal thresholdAmount;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;
    private BigDecimal maxDiscountAmount;
    private Integer totalStock;
    private Integer receivedStock;
    private Integer remainingStock;
    private LocalDateTime claimStartAt;
    private LocalDateTime claimEndAt;
    private LocalDateTime validStartAt;
    private LocalDateTime validEndAt;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
