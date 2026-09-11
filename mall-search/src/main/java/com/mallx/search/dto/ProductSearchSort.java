package com.mallx.search.dto;

/** 前台允许使用的商品搜索排序方式。 */
public enum ProductSearchSort {
    /** 有关键词时优先相关度，无关键词时按销量和 SPU ID 保持稳定顺序。 */
    RELEVANCE,

    /** 按 SPU 在售 SKU 的最低价升序。 */
    PRICE_ASC,

    /** 按 SPU 在售 SKU 的最低价降序。 */
    PRICE_DESC,

    /** 按累计已售库存降序。 */
    SALES_DESC,

    /** 按商品最后更新时间降序。 */
    NEWEST
}
