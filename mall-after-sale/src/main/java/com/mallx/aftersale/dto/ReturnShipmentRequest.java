package com.mallx.aftersale.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

/** 用户寄回整单商品的物流信息。 */
@Data
public class ReturnShipmentRequest {
    @NotBlank(message = "物流公司不能为空")
    @Size(max = 64, message = "物流公司不能超过64字")
    private String carrier;

    @NotBlank(message = "物流单号不能为空")
    @Size(max = 64, message = "物流单号不能超过64位")
    private String trackingNo;
}
