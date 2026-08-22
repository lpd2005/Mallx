package com.mallx.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/** 加入购物车请求。 */
@Data
public class AddCartItemRequest {
    /** 要加入的SKU ID。 */
    @NotNull
    @Positive
    private Long skuId;
    /** 本次增加的数量。 */
    @NotNull
    @Min(1)
    @Max(99)
    private Integer quantity;
}
