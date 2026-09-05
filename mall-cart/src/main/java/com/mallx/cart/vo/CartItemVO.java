package com.mallx.cart.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** 购物车商品展示信息，商品价格始终来自商品模块当前数据。 */
@Data
@Builder
public class CartItemVO {
    /** SKU ID。 */
    private Long skuId;

    /** SPU ID；商品不存在时为空。 */
    private Long spuId;

    /** 商品名称。 */
    private String productName;

    /** SKU名称。 */
    private String skuName;

    /** SKU规格JSON。 */
    private String specJson;

    /** 图片完整访问URL；沿用OssKey字段名，不再保存对象Key。空值表示未设置图片。 */
    private String imageOssKey;

    /** 商品当前销售价，不从Redis购物车读取。 */
    private BigDecimal salePrice;

    /** 商品当前划线价。 */
    private BigDecimal marketPrice;

    /** 购物车数量。 */
    private Integer quantity;

    /** 是否勾选。 */
    private Boolean selected;

    /** 当前可用库存；库存未初始化时为空。 */
    private Integer availableStock;

    /** 当前是否可以进入结算。 */
    private Boolean purchasable;

    /** 不可购买原因；可购买时为空。 */
    private String invalidReason;

    /** 当前价格乘以购物车数量。 */
    private BigDecimal subtotal;

    /** 首次加入购物车时间戳。 */
    private Long addedAt;
}
