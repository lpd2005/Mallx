package com.mallx.coupon.enums;

import lombok.Getter;

/** 优惠券优惠方式。 */
@Getter
public enum CouponType {
    /** 达到门槛后减去固定金额。 */
    FULL_REDUCTION(1),
    /** 达到门槛后按折扣率计算优惠。 */
    DISCOUNT(2);

    /** 数据库存储值。 */
    private final int value;

    CouponType(int value) {
        this.value = value;
    }

    /** 根据数据库值解析优惠券类型，非法值返回 {@code null}。 */
    public static CouponType fromValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (CouponType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        return null;
    }
}
