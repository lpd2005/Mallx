package com.mallx.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 修改购物车SKU数量请求。 */
@Data
public class UpdateCartQuantityRequest {
    /** 修改后的绝对数量，不是增加量。 */
    @NotNull
    @Min(1)
    @Max(99)
    private Integer quantity;
}
