package com.mallx.product.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** 购物车内部使用的商品当前信息，由SPU和SKU联合查询得到。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartProductVO {
    /** SKU ID。 */
    private Long skuId;
    /** SPU ID。 */
    private Long spuId;
    /** SPU商品名称。 */
    private String productName;
    /** SKU展示名称。 */
    private String skuName;
    /** SKU销售规格JSON。 */
    private String specJson;
    /** 优先使用SKU图片，没有时使用SPU主图。 */
    private String imageOssKey;
    /** 当前销售价。 */
    private BigDecimal salePrice;
    /** 当前划线价。 */
    private BigDecimal marketPrice;
    /** SPU状态。 */
    private Integer spuStatus;
    /** SKU状态。 */
    private Integer skuStatus;
}
