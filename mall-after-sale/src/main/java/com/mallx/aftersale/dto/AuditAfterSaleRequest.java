package com.mallx.aftersale.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

/** 审核结果；Boolean只属于接口参数，数据库仍用数值状态。 */
@Data
public class AuditAfterSaleRequest {
    @NotNull(message = "审核结果不能为空")
    private Boolean approved;

    @NotBlank(message = "审核说明不能为空")
    @Size(max = 255, message = "审核说明不能超过255字")
    private String remark;
}
