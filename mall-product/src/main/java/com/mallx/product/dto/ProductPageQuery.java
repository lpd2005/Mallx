package com.mallx.product.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import lombok.Data;

/** 前台商品分页参数，不允许客户端指定商品状态或任意排序SQL。 */
@Data
public class ProductPageQuery {
    /** 页码从1开始，限制上界以避免偏移量溢出。 */
    @Min(value = 1, message = "页码不能小于1")
    @Max(value = 100000, message = "页码不能超过100000")
    private long current = 1;

    @Min(value = 1, message = "每页数量不能小于1")
    @Max(value = 100, message = "每页数量不能超过100")
    private long size = 20;

    /** 精确匹配商品绑定的叶子分类ID，不递归查询子分类。 */
    @Positive(message = "分类ID必须大于0")
    private Long categoryId;

    @Positive(message = "品牌ID必须大于0")
    private Long brandId;

    /** 按商品名称包含匹配，空白字符串不参与筛选。 */
    @Size(max = 120, message = "关键词长度不能超过120位")
    private String keyword;
}
