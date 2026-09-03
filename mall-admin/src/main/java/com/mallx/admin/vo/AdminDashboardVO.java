package com.mallx.admin.vo;

import lombok.*;

import java.math.BigDecimal;
import java.time.*;

/** 指定日期范围的运营数据；日期闭区间，SQL时间使用左闭右开。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardVO {
    /** 开始日期。 */
    private LocalDate startDate;

    /** 结束日期。 */
    private LocalDate endDate;

    /** 当前账号总数。 */
    private Long userCount;

    /** 区间新增账号数。 */
    private Long newUserCount;

    /** 区间创建订单数，含取消单。 */
    private Long orderCount;

    /** 区间下单应付金额合计，含未支付和取消；不是销售额。 */
    private BigDecimal orderAmount;

    /** 区间支付成功笔数，包括后来退款的订单。 */
    private Long paidOrderCount;

    /** 区间支付金额。 */
    private BigDecimal paidAmount;

    /** 区间成功退款金额。 */
    private BigDecimal refundAmount;

    /** 区间支付减退款，允许为负数。 */
    private BigDecimal netReceiptAmount;
}
