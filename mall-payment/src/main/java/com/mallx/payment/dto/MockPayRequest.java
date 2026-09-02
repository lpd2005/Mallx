package com.mallx.payment.dto;

import jakarta.validation.constraints.*;

import lombok.Data;

/** 模拟支付请求，requestNo由客户端为一次支付动作生成并用于幂等。 */
@Data
public class MockPayRequest {
    @NotBlank(message = "支付请求号不能为空")
    @Size(max = 64, message = "支付请求号不能超过64位")
    private String requestNo;
}
