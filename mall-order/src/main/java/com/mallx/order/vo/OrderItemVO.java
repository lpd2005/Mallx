package com.mallx.order.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** 订单商品快照响应。 */
@Data
@Builder
public class OrderItemVO {
    private Long skuId;
    private Long spuId;
    private String productName;
    private String skuName;
    private String specJson;
    private String imageOssKey;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal subtotal;
}
