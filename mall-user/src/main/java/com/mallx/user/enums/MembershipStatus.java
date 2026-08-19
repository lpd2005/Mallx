package com.mallx.user.enums;

import lombok.Getter;

/** 用户会员记录状态。 */
@Getter
public enum MembershipStatus {
    ACTIVE(1),
    EXPIRED(2),
    CANCELED(3);

    /** 数据库存储值。 */
    private final int value;

    MembershipStatus(int value) {
        this.value = value;
    }

    /** 判断数据库状态值是否为当前状态。 */
    public boolean matches(Integer value) {
        return value != null && this.value == value;
    }
}
