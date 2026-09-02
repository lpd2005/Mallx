package com.mallx.aftersale.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

/** 仓库确认整单退货；仅全部商品可再次销售时选择回补。 */
@Data
public class ReceiveReturnRequest {
    @NotNull(message = "是否回补可销售库存不能为空")
    private Boolean restock;

    @NotBlank(message = "验收说明不能为空")
    @Size(max = 255, message = "验收说明不能超过255字")
    private String remark;
}
