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
    @NotNull private Long spuId;

    /** 全局唯一SKU编码。 */
    @NotBlank
    @Size(max = 64)
    private String skuCode;

    /** SKU展示名称。 */
    @NotBlank
    @Size(max = 160)
    private String skuName;

    /** JSON对象格式的销售规格。 */
    @NotBlank private String specJson;

    /** 图片完整访问URL；沿用OssKey字段名，不再保存对象Key。空值表示未设置图片。 */
    @Size(max = 1024, message = "图片地址长度不能超过1024位")
    @org.hibernate.validator.constraints.URL(protocol = "https", message = "图片地址必须为完整HTTPS URL")
    private String imageOssKey;

    /** 销售价。 */
    @NotNull
    @DecimalMin("0.00")
    private BigDecimal salePrice;

    /** 划线价，可为空且不得低于销售价。 */
    @DecimalMin("0.00")
    private BigDecimal marketPrice;
}
