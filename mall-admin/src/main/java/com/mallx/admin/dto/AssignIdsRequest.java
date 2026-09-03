package com.mallx.admin.dto;

import jakarta.validation.constraints.*;

import lombok.Data;

import java.util.List;

/** 全量替换关联；空列表表示清空，null不表示清空。 */
@Data
public class AssignIdsRequest {
    @NotNull
    @Size(max = 100)
    private List<@NotNull @Positive Long> ids;
}
