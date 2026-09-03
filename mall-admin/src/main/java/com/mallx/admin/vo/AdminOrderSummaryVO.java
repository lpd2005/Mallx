package com.mallx.admin.vo;

import lombok.*;

import java.math.BigDecimal;
import java.time.*;

/** 后台订单分页概览，详情另行加载，避免逐单查询明细。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrderSummaryVO {
    /** 订单号。 */
    private String orderNo;

    /** 买家ID。 */
    private Long userId;

    /** 订单状态。 */
    private Integer status;

    /** 应付金额。 */
    private BigDecimal payAmount;

    /** 购买件数。 */
    private Integer totalQuantity;

    /** 创建时间。 */
    private LocalDateTime createdAt;

    /** 支付时间。 */
    private LocalDateTime paidAt;
}
