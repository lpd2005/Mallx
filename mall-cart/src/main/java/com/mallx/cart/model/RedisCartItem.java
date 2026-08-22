package com.mallx.cart.model;

import lombok.Data;

/** Redis中保存的购物车项，只包含购物车自身状态，不保存价格和商品快照。 */
@Data
public class RedisCartItem {
    /** 用户希望购买的数量。 */
    private Integer quantity;
    /** 是否被用户勾选。 */
    private Boolean selected;
    /** 首次加入购物车的毫秒时间戳。 */
    private Long addedAt;
}
