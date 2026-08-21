package com.mallx.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 商品分类，最多使用三级树形结构。 */
@Data
@TableName("product_category")
public class ProductCategory {
    /** 分类主键，由MyBatis-Plus雪花算法生成。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    /** 父分类ID，0表示根分类。 */
    private Long parentId;
    /** 同级范围内唯一的分类名称。 */
    private String categoryName;
    /** 分类层级：1至3。 */
    private Integer categoryLevel;
    /** 展示排序，数值越小越靠前。 */
    private Integer sort;
    /** 分类图标OSS对象Key。 */
    private String iconOssKey;
    /** 是否启用：0否、1是。 */
    private Integer enabled;
    /** 创建时间。 */
    private LocalDateTime createdAt;
    /** 最后更新时间。 */
    private LocalDateTime updatedAt;
}
