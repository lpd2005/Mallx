package com.mallx.seckill.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

/** 用户提交秒杀请求。 */
@Data
public class SeckillPurchaseRequest {
    /** 当前用户名下的收货地址 ID，消费阶段再次校验归属。 */
    @NotNull(message = "收货地址不能为空")
    private Long addressId;

    /** 本次购买数量，还会受到活动限购和剩余库存约束。 */
    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量必须大于0")
    private Integer quantity;
}
