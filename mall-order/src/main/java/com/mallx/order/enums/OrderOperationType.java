package com.mallx.order.enums;

import lombok.Getter;

/** 订单状态操作类型。 */
@Getter
public enum OrderOperationType {
    CREATE(1),
    PAY(2),
    PREPARE_SHIPMENT(3),
    SHIP(4),
    RECEIVE(5),
    CANCEL(6),
    TIMEOUT_CLOSE(7),
    AFTER_SALE_APPLY(8),
    AFTER_SALE_CLOSE(9),
    REFUND(10);
    private final int value;

    OrderOperationType(int value) {
        this.value = value;
    }
}
