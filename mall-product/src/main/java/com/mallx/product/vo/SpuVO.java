package com.mallx.product.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** 前台SPU详情。 */
@Data
@Builder
public class SpuVO {
    /** SPU ID。 */
    private Long id;

    /** 分类ID。 */
    private Long categoryId;

    /** 品牌ID。 */
    private Long brandId;

    /** 商品名称。 */
    private String productName;

    /** 商品副标题。 */
    private String subtitle;

    /** 图片完整访问URL；沿用OssKey字段名，不再保存对象Key。空值表示未设置图片。 */
    private String mainImageOssKey;

    /** 商品描述信息。 */
    private String description;

    /** 已上架SKU最低价。 */
    private BigDecimal minPrice;

    /** 已上架SKU最高价。 */
    private BigDecimal maxPrice;

    /** 商品状态。 */
    private Integer status;
}
