package com.mallx.admin.dto;

import jakarta.validation.constraints.*;

import lombok.Data;

/** 入库单号由调用方生成并在重试时复用，不能每次请求生成新号。 */
@Data
public class StockInboundRequest {
    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9_-]{1,48}")
    private String inboundNo;

    @NotNull @Positive private Integer quantity;

    @Size(max = 255)
    private String remark;
}
