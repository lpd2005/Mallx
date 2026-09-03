package com.mallx.admin.vo;

import lombok.*;

import java.time.*;

/** 按支付时间统计当前未整单退款的SKU销量，不虚构转化率。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminTopProductVO {
    /** SKU ID，按订单快照分组。 */
    private Long skuId;

    /** 订单快照名称。 */
    private String skuName;

    /** 购买件数，不是库存soldStock。 */
    private Long quantity;
}
