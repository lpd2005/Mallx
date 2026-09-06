package com.mallx.product.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/** 前台SKU详情。 */
@Data
@Builder
public class SkuVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    /** SKU ID。 */
    private Long id;

    /** 所属SPU ID。 */
    private Long spuId;

    /** SKU业务编码。 */
    private String skuCode;

    /** SKU名称。 */
    private String skuName;

    /** 销售规格JSON。 */
    private String specJson;

    /** 图片完整访问URL；沿用OssKey字段名，不再保存对象Key。空值表示未设置图片。 */
    private String imageOssKey;

    /** 销售价。 */
    private BigDecimal salePrice;

    /** 划线价。 */
    private BigDecimal marketPrice;

    /** SKU状态。 */
    private Integer status;
}
