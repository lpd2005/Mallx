package com.mallx.product.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** 前台分类树节点。 */
@Data
@Builder
public class CategoryVO {
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
    /** 图标OSS对象Key。 */
    private String iconOssKey;
    /** 子分类节点。 */
    private List<CategoryVO> children;
}
