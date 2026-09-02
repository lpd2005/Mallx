package com.mallx.order.dto;

import jakarta.validation.constraints.*;

import lombok.Data;

/** 用户取消订单请求。 */
@Data
public class CancelOrderRequest {
    @NotBlank(message = "取消原因不能为空")
    @Size(max = 255, message = "取消原因不能超过255个字符")
    private String reason;
}
