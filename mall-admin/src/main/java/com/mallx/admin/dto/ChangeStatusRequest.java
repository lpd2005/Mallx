package com.mallx.admin.dto;

import jakarta.validation.constraints.NotNull;

import lombok.Data;

/** 状态码由对应领域服务校验，账号0/1，商品0/1/2。 */
@Data
public class ChangeStatusRequest {
    @NotNull private Integer status;
}
