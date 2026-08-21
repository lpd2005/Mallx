package com.mallx.inventory.enums;

import lombok.Getter;

/** 库存流水操作类型。 */
@Getter
public enum InventoryOperationType {
    /** 入库或盘点增加可用库存。 */
    INCREASE_AVAILABLE(1),
    /** 下单占用可用库存。 */
    LOCK(2),
    /** 支付或履约确认后最终扣减锁定库存。 */
    DEDUCT(3),
    /** 订单取消后释放锁定库存。 */
    RELEASE(4);

    /** 数据库存储值。 */
    private final int value;

    InventoryOperationType(int value) { this.value = value; }
}
