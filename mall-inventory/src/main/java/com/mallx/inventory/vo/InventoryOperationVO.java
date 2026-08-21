package com.mallx.inventory.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** 库存操作流水响应。 */
@Data
@Builder
public class InventoryOperationVO {
    /** 业务单号。 */
    private String businessNo;
    /** SKU ID。 */
    private Long skuId;
    /** 操作类型。 */
    private Integer operationType;
    /** 操作数量。 */
    private Integer quantity;
    /** 操作前可用库存。 */
    private Integer beforeAvailable;
    /** 操作后可用库存。 */
    private Integer afterAvailable;
    /** 操作前锁定库存。 */
    private Integer beforeLocked;
    /** 操作后锁定库存。 */
    private Integer afterLocked;
    /** 操作前已售库存。 */
    private Integer beforeSold;
    /** 操作后已售库存。 */
    private Integer afterSold;
    /** 流水创建时间。 */
    private LocalDateTime createdAt;
}
