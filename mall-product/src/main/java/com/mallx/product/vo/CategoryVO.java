package com.mallx.product.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/** 前台分类树节点。 */
@Data
@Builder
public class CategoryVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    /** 分类ID。 */
    private Long id;

    /** 父分类ID。 */
    private Long parentId;

    /** 分类名称。 */
    private String categoryName;

    /** 分类层级。 */
    private Integer categoryLevel;

    /** 展示排序。 */
    private Integer sort;

    /** 图片完整访问URL；沿用OssKey字段名，不再保存对象Key。空值表示未设置图片。 */
    private String iconOssKey;

    /** 子分类节点。 */
    private List<CategoryVO> children;
}
