package com.mallx.payment.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 支付单响应。 */
@Data
@Builder
public class PaymentVO {
    private String paymentNo;
    private String orderNo;
    private BigDecimal amount;
    private Integer channel;
    private Integer status;
    private LocalDateTime paidAt;
    private LocalDateTime closedAt;
    private LocalDateTime refundedAt;
    private LocalDateTime createdAt;
}
