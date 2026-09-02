package com.mallx.order.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 支付模块读取的订单事实信息。 */
@Data
@Builder
public class OrderPaymentInfoVO {
    private String orderNo;
    private Long userId;
    private Integer status;
    private BigDecimal payAmount;
    private LocalDateTime expireAt;
}
