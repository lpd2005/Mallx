package com.mallx.search.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/** 商品搜索参数，只开放固定筛选和排序能力。 */
@Data
public class ProductSearchQuery {
    /** 搜索关键词；为空时退化为筛选和排序查询。 */
    @Size(max = 120, message = "关键词长度不能超过120位")
    private String keyword;

    /** 分类路径中的任意一级分类 ID。 */
    @Positive(message = "分类ID必须大于0")
    private Long categoryId;

    /** 精确匹配的品牌 ID。 */
    @Positive(message = "品牌ID必须大于0")
    private Long brandId;

    /** 同一在售 SKU 必须满足的最低销售价。 */
    @DecimalMin(value = "0.00", message = "最低价格不能小于0")
    private BigDecimal minPrice;

    /** 同一在售 SKU 必须满足的最高销售价。 */
    @DecimalMin(value = "0.00", message = "最高价格不能小于0")
    private BigDecimal maxPrice;

    /** true 只查有货商品，false 只查所有 SKU 均无货的商品，null 不限制库存。 */
    private Boolean inStock;

    /** 排序策略，默认根据搜索相关度排序。 */
    private ProductSearchSort sort = ProductSearchSort.RELEVANCE;

    /** 从 1 开始的普通页码。 */
    @Min(value = 1, message = "页码不能小于1")
    @Max(value = 500, message = "普通分页不能超过500页")
    private int current = 1;

    /** 每页记录数；与页码共同受 ES 结果窗口限制。 */
    @Min(value = 1, message = "每页数量不能小于1")
    @Max(value = 50, message = "每页数量不能超过50")
    private int size = 20;
}
