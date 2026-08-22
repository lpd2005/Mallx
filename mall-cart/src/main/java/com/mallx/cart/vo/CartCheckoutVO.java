package com.mallx.cart.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** 购物车结算预览，不代表最终订单成交价格或库存占用。 */
@Data
@Builder
public class CartCheckoutVO {
    /** 已勾选且通过商品、库存校验的购物车项。 */
    private List<CartItemVO> items;
    /** 结算商品总件数。 */
    private Integer totalQuantity;
    /** 按商品当前价格计算的原始总额。 */
    private BigDecimal productAmount;
}
