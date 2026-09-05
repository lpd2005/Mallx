package com.mallx.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.time.LocalDateTime;

/** 商品品牌。 */
@Data
@TableName("product_brand")
public class ProductBrand {
    /** 品牌主键。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 品牌名称。 */
    private String brandName;

    /** 图片完整访问URL；沿用OssKey字段名，不再保存对象Key。空值表示未设置图片。 */
    private String logoOssKey;

    /** 品牌介绍。 */
    private String description;

    /** 展示排序。 */
    private Integer sort;

    /** 是否启用：0否、1是。 */
    private Integer enabled;

    /** 创建时间。 */
    private LocalDateTime createdAt;

    /** 最后更新时间。 */
    private LocalDateTime updatedAt;
}
