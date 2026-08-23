package com.mallx.coupon.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 创建或修改优惠券模板的请求参数。 */
@Data
public class CouponTemplateRequest {
    @NotBlank(message = "优惠券名称不能为空")
    @Size(max = 100, message = "优惠券名称不能超过100个字符")
    private String couponName;
    /** 1满减券，2折扣券。 */
    @NotNull(message = "优惠券类型不能为空")
    private Integer couponType;
    @NotNull(message = "使用门槛不能为空")
    @DecimalMin(value = "0.00", message = "使用门槛不能小于0")
    private BigDecimal thresholdAmount;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;
    private BigDecimal maxDiscountAmount;
    @NotNull(message = "发行数量不能为空")
    @Min(value = 1, message = "发行数量必须大于0")
    private Integer totalStock;
    @NotNull(message = "领取开始时间不能为空")
    private LocalDateTime claimStartAt;
    @NotNull(message = "领取结束时间不能为空")
    private LocalDateTime claimEndAt;
    @NotNull(message = "使用开始时间不能为空")
    private LocalDateTime validStartAt;
    @NotNull(message = "使用结束时间不能为空")
    private LocalDateTime validEndAt;
}
