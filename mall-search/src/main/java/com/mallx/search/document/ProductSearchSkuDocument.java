package com.mallx.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;

/** SPU 搜索文档中的可售 SKU 快照。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchSkuDocument {
    /** SKU 主键。 */
    @Field(type = FieldType.Long)
    private Long skuId;

    /** SKU 展示名称，参与 nested 全文检索。 */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String skuName;

    /** 由规格 JSON 转成的可搜索文本。 */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String specText;

    /** 当前销售价；scaled_float 以分为精度存储。 */
    @Field(type = FieldType.Scaled_Float, scalingFactor = 100)
    private BigDecimal salePrice;

    /** 普通订单当前可使用的库存数量。 */
    @Field(type = FieldType.Integer)
    private Integer availableStock;

    /** 已售数量，用于汇总 SPU 销量。 */
    @Field(type = FieldType.Integer)
    private Integer soldStock;
}
