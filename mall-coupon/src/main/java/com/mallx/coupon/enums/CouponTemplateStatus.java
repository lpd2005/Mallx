package com.mallx.coupon.enums;

import lombok.Getter;

/** 优惠券模板状态。 */
@Getter
public enum CouponTemplateStatus {
    DRAFT(0),
    ACTIVE(1),
    STOPPED(2),
    ENDED(3);

    /** 数据库存储值。 */
    private final int value;

    CouponTemplateStatus(int value) {
        this.value = value;
    }

    /** 判断数据库状态值是否为当前状态。 */
    public boolean matches(Integer value) {
        return value != null && this.value == value;
    }
}
