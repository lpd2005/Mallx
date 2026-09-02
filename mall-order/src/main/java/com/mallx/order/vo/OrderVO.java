package com.mallx.order.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 订单详情响应。 */
@Data
@Builder
public class OrderVO {
    private String orderNo;
    private Integer status;
    private Integer totalQuantity;
    private BigDecimal productAmount;
    private BigDecimal memberDiscountAmount;
    private BigDecimal couponDiscountAmount;
    private BigDecimal payAmount;
    private Long couponUserId;
    private Integer memberLevelCode;
    private BigDecimal memberDiscountRate;
    private String recipientName;
    private String recipientMobile;
    private String fullAddress;
    private String postalCode;
    private String remark;
    private LocalDateTime expireAt;
    private LocalDateTime paidAt;
    private LocalDateTime shippedAt;
    private LocalDateTime completedAt;
    private LocalDateTime canceledAt;
    private String cancelReason;
    private LocalDateTime createdAt;
    private List<OrderItemVO> items;
}
