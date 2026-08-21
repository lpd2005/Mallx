package com.mallx.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/** 新增或更新SKU请求。 */
@Data
public class SkuRequest {
    /** 所属SPU ID。 */
    @NotNull
    private Long spuId;
    /** 全局唯一SKU编码。 */
    @NotBlank
    @Size(max = 64)
    private String skuCode;
    /** SKU展示名称。 */
    @NotBlank
    @Size(max = 160)
    private String skuName;
    /** JSON对象格式的销售规格。 */
    @NotBlank
    private String specJson;
    /** SKU图片OSS对象Key。 */
    @Size(max = 255)
    private String imageOssKey;
    /** 销售价。 */
    @NotNull
    @DecimalMin("0.00")
    private BigDecimal salePrice;
    /** 划线价，可为空且不得低于销售价。 */
    @DecimalMin("0.00")
    private BigDecimal marketPrice;
}
