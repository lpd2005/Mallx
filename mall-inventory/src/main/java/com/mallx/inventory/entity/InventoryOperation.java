package com.mallx.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 库存操作流水，同时通过业务唯一键承担幂等控制。 */
@Data
@TableName("inventory_operation")
public class InventoryOperation {
    /** 流水主键，由服务层生成雪花ID。 */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    /** 入库单号、订单号等幂等业务标识。 */
    private String businessNo;
    /** 被操作的SKU。 */
    private Long skuId;
    /** 操作类型：1增加、2锁定、3扣减、4释放。 */
    private Integer operationType;
    /** 本次变化数量。 */
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
    /** 流水状态：0处理中、1成功。 */
    private Integer status;
    /** 操作说明。 */
    private String remark;
    /** 创建时间。 */
    private LocalDateTime createdAt;
    /** 最后更新时间。 */
    private LocalDateTime updatedAt;
}
