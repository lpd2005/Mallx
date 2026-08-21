package com.mallx.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 标准商品单元，描述一组共享商品信息的SKU。 */
@Data
@TableName("product_spu")
public class ProductSpu {
    /** SPU主键。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    /** 所属商品分类。 */
    private Long categoryId;
    /** 所属品牌。 */
    private Long brandId;
    /** 商品名称。 */
    private String productName;
    /** 商品副标题。 */
    private String subtitle;
    /** 商品主图OSS对象Key。 */
    private String mainImageOssKey;
    /** 商品描述信息，最多1000个字符。 */
    private String description;
    /** 已上架SKU最低销售价。 */
    private BigDecimal minPrice;
    /** 已上架SKU最高销售价。 */
    private BigDecimal maxPrice;
    /** 状态：0草稿、1上架、2下架。 */
    private Integer status;
    /** 创建时间。 */
    private LocalDateTime createdAt;
    /** 最后更新时间。 */
    private LocalDateTime updatedAt;
}
