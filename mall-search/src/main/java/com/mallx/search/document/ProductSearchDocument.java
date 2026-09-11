package com.mallx.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 面向前台搜索的一份 SPU 聚合文档，MySQL 仍是事实来源。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "#{@productSearchProperties.indexAlias}", createIndex = false)
public class ProductSearchDocument {
    /** 使用 SPU ID 作为文档 ID，重复同步天然变成覆盖写。 */
    @Id
    private Long spuId;

    /** 主搜索字段，建立索引时细粒度分词，查询时使用较稳健的智能分词。 */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String productName;

    /** 商品卖点副标题，参与全文检索和高亮。 */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String subtitle;

    /** 商品描述参与召回，但当前列表接口不返回完整描述。 */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String description;

    /** 图片 URL 只作为展示数据保存，不参与搜索。 */
    @Field(type = FieldType.Keyword, index = false)
    private String mainImageUrl;

    /** 品牌 ID 用于精确过滤。 */
    @Field(type = FieldType.Long)
    private Long brandId;

    /** 品牌名称既参与全文搜索，也用于结果展示。 */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String brandName;

    /** SPU 直接所属的叶子分类 ID。 */
    @Field(type = FieldType.Long)
    private Long categoryId;

    /** 冗余完整分类路径，使父分类也能筛选其所有后代商品。 */
    @Field(type = FieldType.Long)
    private List<Long> categoryPathIds;

    /** 分类路径名称参与全文搜索。 */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private List<String> categoryPathNames;

    /** 在售 SKU 最低价，主要用于列表展示和排序。 */
    @Field(type = FieldType.Scaled_Float, scalingFactor = 100)
    private BigDecimal minPrice;

    /** 在售 SKU 最高价，主要用于列表展示。 */
    @Field(type = FieldType.Scaled_Float, scalingFactor = 100)
    private BigDecimal maxPrice;

    /** 是否至少存在一个可售库存大于 0 的 SKU。 */
    @Field(type = FieldType.Boolean)
    private Boolean hasStock;

    /** 所有在售 SKU 已售数量之和，用于销量排序。 */
    @Field(type = FieldType.Long)
    private Long soldCount;

    /** SPU 最后更新时间，用于新品排序。 */
    @Field(type = FieldType.Date)
    private LocalDateTime updatedAt;

    /**
     * 在售 SKU 快照必须使用 nested 类型，保证价格、库存和规格条件命中同一个 SKU，
     * 避免普通 object 数组的字段交叉匹配。
     */
    @Field(type = FieldType.Nested) // 嵌套类型
    private List<ProductSearchSkuDocument> skus;
}
