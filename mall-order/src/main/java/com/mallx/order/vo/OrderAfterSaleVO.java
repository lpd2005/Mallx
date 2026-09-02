package com.mallx.order.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** 订单进入售后时返回的可信快照，不使用前端提交的退款金额。 */
@Data
@Builder
public class OrderAfterSaleVO {
    private Long orderId;
    private String orderNo;
    private Integer originalStatus;
    private BigDecimal payAmount;
}
