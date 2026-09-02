package com.mallx.payment.enums;

import lombok.Getter;

/** 支付单状态。 */
@Getter
public enum PaymentStatus {
//    '支付状态：1待支付，2已支付，3已关闭，4已退款'
    PENDING(1),
    PAID(2),
    CLOSED(3),
    REFUNDED(4);
    private final int value;

    PaymentStatus(int value) {
        this.value = value;
    }

    public boolean matches(Integer value) {
        return value != null && this.value == value;
    }
}
