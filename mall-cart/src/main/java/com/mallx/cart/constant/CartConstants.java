package com.mallx.cart.constant;

import java.time.Duration;

/** 购物车容量、数量和Redis生命周期约束。 */
public final class CartConstants {
    /** 每个用户最多保存的SKU种类。 */
    public static final int MAX_ITEM_KINDS = 100;
    /** 单个SKU最多购买数量。 */
    public static final int MAX_ITEM_QUANTITY = 99;
    /** 用户最后一次访问后购物车保留30天。 */
    public static final Duration EXPIRE_DURATION = Duration.ofDays(30);

    private CartConstants() {
    }
}
