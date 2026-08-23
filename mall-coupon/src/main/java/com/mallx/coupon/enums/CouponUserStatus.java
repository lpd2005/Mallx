package com.mallx.coupon.enums;

import lombok.Getter;

/** 用户优惠券状态。 */
@Getter
public enum CouponUserStatus {
    UNUSED(1),
    LOCKED(2),
    USED(3),
    EXPIRED(4);

    /** 数据库存储值。 */
    private final int value;

    CouponUserStatus(int value) {
        this.value = value;
    }

    /** 判断数据库状态值是否为当前状态。 */
    public boolean matches(Integer value) {
        return value != null && this.value == value;
    }

    /** 判断是否为支持的数据库状态值。 */
    public static boolean supports(Integer value) {
        for (CouponUserStatus status : values()) {
            if (status.matches(value)) {
                return true;
            }
        }
        return false;
    }
}
