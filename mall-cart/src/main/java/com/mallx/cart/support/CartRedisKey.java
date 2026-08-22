package com.mallx.cart.support;

/** 购物车Redis Key生成器。 */
public final class CartRedisKey {
    private static final String CART_KEY_PREFIX = "mall:cart:";

    private CartRedisKey() {
    }

    /** 生成按用户隔离的购物车Key。 */
    public static String cartKey(Long userId) {
        return CART_KEY_PREFIX + "{" + userId + "}";
    }
}
