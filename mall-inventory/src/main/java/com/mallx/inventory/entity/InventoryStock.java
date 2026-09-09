package com.mallx.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** SKU库存聚合，始终保证各库存数量非负。 */
@Data
@TableName("inventory_stock")
public class InventoryStock {
    /** SKU ID，同时作为库存表主键。 */
    @TableId(value = "sku_id", type = IdType.INPUT)
    private Long skuId;
    /** 可以继续下单的库存数量。 */
    private Integer availableStock;
    /** 已被订单占用或被秒杀活动预留、但尚未最终扣减的数量。 */
    private Integer lockedStock;
    /** 已完成最终扣减的累计数量。 */
    private Integer soldStock;
    /** 库存记录创建时间。 */
    private LocalDateTime createdAt;
    /** 最后更新时间。 */
    private LocalDateTime updatedAt;
}
