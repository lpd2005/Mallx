package com.mallx.user.enums;

import lombok.Getter;

/** MallX 当前支持的两种真实会员等级。 */
@Getter
public enum MemberLevelCode {
    /** 普通会员，数据库值为1，享受95折。 */
    NORMAL(1),
    /** VIP会员，数据库值为2，享受8折且必须设置到期时间。 */
    VIP(2);

    /** 数据库存储值。 */
    private final int value;

    MemberLevelCode(int value) {
        this.value = value;
    }

    /** 判断数据库值是否为当前会员等级。 */
    public boolean matches(Integer value) {
        return value != null && this.value == value;
    }

    /** 根据数据库值获取会员等级，非法值返回 {@code null}。 */
    public static MemberLevelCode fromValue(Integer value) {
        if (NORMAL.matches(value)) {
            return NORMAL;
        }
        if (VIP.matches(value)) {
            return VIP;
        }
        return null;
    }
}
