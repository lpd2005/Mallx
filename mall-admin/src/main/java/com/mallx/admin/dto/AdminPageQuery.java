package com.mallx.admin.dto;

import jakarta.validation.constraints.*;

import lombok.Data;

/** 后台通用分页条件。keyword用于名称/手机号模糊查询，status用于状态筛选。 */
@Data
public class AdminPageQuery {
    @Min(1)
    private long current = 1;

    @Min(1)
    @Max(100)
    private long size = 20;

    @Size(max = 128)
    private String keyword;

    private Integer status;
}
