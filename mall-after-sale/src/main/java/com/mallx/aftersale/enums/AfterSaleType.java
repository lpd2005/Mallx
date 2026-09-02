package com.mallx.aftersale.enums;

import lombok.Getter;

/** 售后类型，持久化为TINYINT。 */
@Getter
public enum AfterSaleType {
    REFUND_ONLY(1),
    RETURN_REFUND(2);

    private final int value;

    AfterSaleType(int value) {
        this.value = value;
    }

    public static AfterSaleType fromValue(Integer value) {
        for (AfterSaleType type : values()) {
            if (value != null && type.value == value) {
                return type;
            }
        }
        return null;
    }
}
