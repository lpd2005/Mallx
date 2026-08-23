package com.mallx.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 优惠券领取和状态变化流水，用于审计、幂等与问题追踪。 */
@Data
@TableName("coupon_operation_log")
public class CouponOperationLog {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    private String operationNo;
    private Long userCouponId;
    private Long userId;
    private String orderNo;
    /** 1领取，2锁定，3使用，4回滚，5过期。 */
    private Integer operationType;
    private Integer beforeStatus;
    private Integer afterStatus;
    private String remark;
    private LocalDateTime createdAt;
}
