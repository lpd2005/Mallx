package com.mallx.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 可供运营配置和发布的优惠券模板。 */
@Data
@TableName("coupon_template")
public class CouponTemplate {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    private String couponName;
    /** 1满减券，2折扣券。 */
    private Integer couponType;
    private BigDecimal thresholdAmount;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;
    private BigDecimal maxDiscountAmount;
    private Integer totalStock;
    private Integer receivedStock;
    private LocalDateTime claimStartAt;
    private LocalDateTime claimEndAt;
    private LocalDateTime validStartAt;
    private LocalDateTime validEndAt;
    /** 0草稿，1发放中，2已停止，3已结束。 */
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
