package com.mallx.admin.dto;

import jakarta.validation.constraints.*;

import lombok.Data;

/** 后台只能上架或下架，草稿仅在创建时产生，禁止退回草稿绕过删除限制。 */
@Data
public class ProductStatusRequest {
    @NotNull
    @Min(1)
    @Max(2)
    private Integer status;
}
