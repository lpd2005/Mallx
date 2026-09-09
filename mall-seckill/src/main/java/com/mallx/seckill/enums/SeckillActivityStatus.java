package com.mallx.seckill.enums;

import lombok.Getter;

/** 秒杀活动生命周期。 */
@Getter
public enum SeckillActivityStatus {
    /** 可编辑但尚未写入 Redis。 */
    DRAFT(0),
    /** 已完成 Redis 预热，可按时间窗口接受请求。 */
    READY(1),
    /** 已人工关闭，不再接受新请求。 */
    CLOSED(2);

    /** 数据库存储值。 */
    private final int value;

    SeckillActivityStatus(int value) {
        this.value = value;
    }

    /** 判断数据库状态值是否对应当前枚举。 */
    public boolean matches(Integer status) {
        return status != null && status == value;
    }
}
