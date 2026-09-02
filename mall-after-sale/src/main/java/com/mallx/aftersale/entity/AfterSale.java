package com.mallx.aftersale.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 整单售后申请，不使用逻辑删除；驳回和撤销保留历史记录。 */
@Data
@TableName("after_sale")
public class AfterSale {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 对外售后单号。 */
    private String afterSaleNo;

    private Long orderId;
    private String orderNo;
    private Long userId;

    /** 1仅退款、2退货退款。 */
    private Integer type;

    /** 状态值见AfterSaleStatus。 */
    private Integer status;

    /** 有效售后占位键；完成退款后仍保留，驳回或撤销时置空允许重新申请。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String activeOrderNo;

    /** 进入售后前的订单状态快照。 */
    private Integer originalOrderStatus;

    /** 原订单实际应付金额快照。 */
    private BigDecimal refundAmount;

    private String reason;
    private String auditRemark;
    private Long auditorId;

    /** 0不回补、1回补；退货退款在仓库验收时决定，验收前为空。 */
    private Integer restock;

    private String returnCarrier;
    private String returnTrackingNo;
    private String refundNo;
    private LocalDateTime auditedAt;
    private LocalDateTime returnShippedAt;
    private LocalDateTime receivedAt;
    private LocalDateTime completedAt;
    private LocalDateTime canceledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
