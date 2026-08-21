package com.mallx.product.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** 前台SKU详情。 */
@Data
@Builder
public class SkuVO {
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
    /** 图片OSS对象Key。 */
    private String imageOssKey;
    /** 销售价。 */
    private BigDecimal salePrice;
    /** 划线价。 */
    private BigDecimal marketPrice;
    /** SKU状态。 */
    private Integer status;
}
