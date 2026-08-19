package com.mallx.user.enums;

import lombok.Getter;

/** 会员状态变更类型，用于形成可审计的会员履历。 */
@Getter
public enum MembershipChangeType {
    OPEN(1),
    UPGRADE(2),
    CHANGE(3),
    RENEW(4),
    CANCEL(5),
    EXPIRE(6);

    /** 数据库存储值。 */
    private final int value;

    MembershipChangeType(int value) {
        this.value = value;
    }
}
