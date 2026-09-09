package com.mallx.seckill.enums;

import lombok.Getter;

/** 秒杀请求异步处理状态。 */
@Getter
public enum SeckillRequestStatus {
    /** Redis 预扣成功，正式订单尚未确认创建。 */
    PENDING(0),
    /** 正式订单已经创建。 */
    SUCCESS(1),
    /** 确定性业务失败且资格库存已经补偿。 */
    FAILED(2);

    /** Redis 和数据库共用的存储值。 */
    private final int value;

    SeckillRequestStatus(int value) {
        this.value = value;
    }

    /** 判断存储值是否对应当前枚举。 */
    public boolean matches(Integer status) {
        return status != null && status == value;
    }
}
