package com.mallx.user.enums;

import lombok.Getter;

/** 用户资料中的性别枚举。 */
@Getter
public enum Gender {
    /** 未设置或不愿透露。 */
    UNKNOWN(0),
    /** 男。 */
    MALE(1),
    /** 女。 */
    FEMALE(2);

    /** 数据库存储值。 */
    private final int value;

    Gender(int value) {
        this.value = value;
    }

    /**
     * 判断请求中的性别值是否属于已定义枚举。
     *
     * @param value 待校验的整数值
     * @return 值有效时返回 {@code true}
     */
    public static boolean isValid(Integer value) {
        return value != null && value >= UNKNOWN.value && value <= FEMALE.value;
    }
}
