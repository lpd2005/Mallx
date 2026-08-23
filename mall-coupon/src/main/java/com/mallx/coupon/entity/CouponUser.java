package com.mallx.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 用户领取后实际持有的一张优惠券。 */
@Data
@TableName("coupon_user")
public class CouponUser {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    private Long templateId;
    private Long userId;
    /** 1未使用，2已锁定，3已使用，4已过期。 */
    private Integer status;
    private String orderNo;
    private LocalDateTime receivedAt;
    /** 领取时保存有效期，避免模板后续状态变化影响已领取优惠券。 */
    private LocalDateTime validStartAt;
    private LocalDateTime validEndAt;
    private LocalDateTime lockedAt;
    private LocalDateTime usedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
