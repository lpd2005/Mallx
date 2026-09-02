package com.mallx.order.enums;

import lombok.Getter;

/** 普通订单状态。 */
@Getter
public enum OrderStatus {
    WAIT_PAY(1),
    PAID(2),
    WAIT_SHIP(3),
    SHIPPED(4),
    COMPLETED(5),
    CANCELED(6);
    private final int value;

    OrderStatus(int value) {
        this.value = value;
    }

    public boolean matches(Integer value) {
        return value != null && this.value == value;
    }

    public static boolean supports(Integer value) {
        for (OrderStatus s : values()) if (s.matches(value)) return true;
        return false;
    }
}
