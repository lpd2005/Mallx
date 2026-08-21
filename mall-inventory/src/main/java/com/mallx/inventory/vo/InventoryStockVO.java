package com.mallx.inventory.vo;

import lombok.Builder;
import lombok.Data;

/** SKU当前库存响应。 */
@Data
@Builder
public class InventoryStockVO {
    /** SKU ID。 */
    private Long skuId;
    /** 可用库存。 */
    private Integer availableStock;
    /** 锁定库存。 */
    private Integer lockedStock;
    /** 已售库存。 */
    private Integer soldStock;
    /** 可用库存是否为0。 */
    private Boolean outOfStock;
}
