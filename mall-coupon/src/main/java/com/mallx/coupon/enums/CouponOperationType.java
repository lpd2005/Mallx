package com.mallx.coupon.enums;

import lombok.Getter;

/** 优惠券状态变更类型。 */
@Getter
public enum CouponOperationType {
    CLAIM(1),
    LOCK(2),
    USE(3),
    ROLLBACK(4),
    EXPIRE(5);

    /** 数据库存储值。 */
    private final int value;

    CouponOperationType(int value) {
        this.value = value;
    }
}
