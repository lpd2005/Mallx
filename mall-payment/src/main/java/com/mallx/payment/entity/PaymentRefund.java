package com.mallx.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 模拟整单退款成功流水；失败随本地事务回滚，不伪造第三方退款状态。 */
@Data
@TableName("payment_refund")
public class PaymentRefund {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 退款单号。 */
    private String refundNo;

    /** 售后单号，同时作为退款幂等键。 */
    private String afterSaleNo;

    private String paymentNo;

    /** 当前仅支持整单退款，一个订单只能产生一条成功退款流水。 */
    private String orderNo;

    private Long userId;

    /** 原支付单实际应付金额，不退商品原价或优惠券面值。 */
    private BigDecimal amount;

    private LocalDateTime refundedAt;
    private LocalDateTime createdAt;
}
