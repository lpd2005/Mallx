package com.mallx.cart.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 修改购物车勾选状态请求。 */
@Data
public class ChangeCartSelectionRequest {
    /** true表示勾选，false表示取消勾选。 */
    @NotNull
    private Boolean selected;
}
