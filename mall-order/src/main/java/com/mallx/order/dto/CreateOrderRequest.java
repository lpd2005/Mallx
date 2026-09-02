package com.mallx.order.dto;

import jakarta.validation.constraints.*;

import lombok.Data;

/** 从购物车已勾选商品创建订单。 */
@Data
public class CreateOrderRequest {
    @NotNull(message = "收货地址不能为空")
    @Positive(message = "收货地址ID不正确")
    private Long addressId;

    @Positive(message = "用户优惠券ID不正确")
    private Long couponUserId;

    @Size(max = 255, message = "订单备注不能超过255个字符")
    private String remark;
}
