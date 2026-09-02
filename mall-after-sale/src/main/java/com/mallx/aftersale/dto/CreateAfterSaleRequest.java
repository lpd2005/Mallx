package com.mallx.aftersale.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

/** 只提交订单、类型和原因，退款金额由服务端读取。 */
@Data
public class CreateAfterSaleRequest {
    @NotBlank(message = "订单号不能为空")
    @Size(max = 64, message = "订单号不能超过64位")
    private String orderNo;

    @NotNull(message = "售后类型不能为空")
    private Integer type;

    @NotBlank(message = "售后原因不能为空")
    @Size(max = 500, message = "售后原因不能超过500字")
    private String reason;
}
