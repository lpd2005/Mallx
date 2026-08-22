package com.mallx.cart.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** 当前用户购物车汇总。 */
@Data
@Builder
public class CartVO {
    /** 按加入时间倒序排列的购物车项。 */
    private List<CartItemVO> items;
    /** 不同SKU种类数。 */
    private Integer itemKinds;
    /** 全部SKU数量合计。 */
    private Integer totalQuantity;
    /** 已勾选且可购买的SKU数量合计。 */
    private Integer selectedQuantity;
    /** 已勾选且可购买商品的当前金额合计。 */
    private BigDecimal selectedAmount;
}
