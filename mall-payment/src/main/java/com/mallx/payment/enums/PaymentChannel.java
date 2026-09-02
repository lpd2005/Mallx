package com.mallx.payment.enums;

import lombok.Getter;

/** 当前支持的支付渠道。 */
@Getter
public enum PaymentChannel {
    MOCK(1);
    private final int value;

    PaymentChannel(int value) {
        this.value = value;
    }
}
