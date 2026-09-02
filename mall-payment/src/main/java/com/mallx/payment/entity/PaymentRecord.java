package com.mallx.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 独立支付单，当前只实现模拟支付渠道。 */
@Data
@TableName("payment_record")
public class PaymentRecord {
    /** 支付记录主键。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 支付单号。 */
    private String paymentNo;

    /** 关联订单号；当前一个订单只创建一个支付单。 */
    private String orderNo;

    /** 支付用户ID。 */
    private Long userId;

    /** 应付金额快照。 */
    private BigDecimal amount;

    /** 支付渠道，见 {@code PaymentChannel}。 */
    private Integer channel;

    /** 支付状态，见 {@code PaymentStatus}。 */
    private Integer status;

    /** 模拟支付请求幂等号。 */
    private String paidRequestNo;

    private LocalDateTime paidAt;
    private LocalDateTime closedAt;
    private LocalDateTime refundedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
