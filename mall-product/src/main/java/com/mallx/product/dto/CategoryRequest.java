package com.mallx.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 新增或更新商品分类请求。 */
@Data
public class CategoryRequest {
    /** 父分类ID；根分类传0。 */
    @NotNull
    @PositiveOrZero
    private Long parentId;
    /** 分类名称。 */
    @NotBlank
    @Size(max = 50)
    private String categoryName;
    /** 展示排序。 */
    @NotNull
    @PositiveOrZero
    private Integer sort;
    /** 分类图标OSS对象Key。 */
    @Size(max = 255)
    private String iconOssKey;
    /** 是否启用：0否、1是。 */
    @NotNull
    private Integer enabled;
}
