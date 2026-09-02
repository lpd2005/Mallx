package com.mallx.order.enums;

import lombok.Getter;

/** 普通订单状态。 */
@Getter
public enum OrderStatus {
//    订单状态：1待支付，2已支付，3待发货，4已发货，5已完成，6已取消，7售后中，8已退款',
    WAIT_PAY(1),
    PAID(2),
    WAIT_SHIP(3),
    SHIPPED(4),
    COMPLETED(5),
    CANCELED(6),
    /** 售后处理中，暂停履约。 */
    AFTER_SALE(7),
    /** 整单退款完成。 */
    REFUNDED(8);
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
