package com.mallx.search.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** 商品搜索结果；高亮文本包含 em 标签，前端展示前仍需进行白名单过滤。 */
@Data
@Builder
public class ProductSearchVO {
    /** 商品 SPU ID。 */
    private Long spuId;
    /** 未高亮的原始商品名称。 */
    private String productName;
    /** 未高亮的原始副标题。 */
    private String subtitle;
    /** ES 返回的商品名称高亮片段，无命中时回退为原始名称。 */
    private String highlightedProductName;
    /** ES 返回的副标题高亮片段，无命中时回退为原始副标题。 */
    private String highlightedSubtitle;
    /** 商品主图访问地址。 */
    private String mainImageUrl;
    /** 品牌 ID。 */
    private Long brandId;
    /** 品牌名称。 */
    private String brandName;
    /** 叶子分类 ID。 */
    private Long categoryId;
    /** 叶子分类名称。 */
    private String categoryName;
    /** 在售 SKU 最低价。 */
    private BigDecimal minPrice;
    /** 在售 SKU 最高价。 */
    private BigDecimal maxPrice;
    /** 是否存在可售库存。 */
    private Boolean hasStock;
    /** 汇总销量。 */
    private Long soldCount;
}
