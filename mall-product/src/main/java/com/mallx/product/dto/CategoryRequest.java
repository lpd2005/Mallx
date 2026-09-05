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
    @NotNull @PositiveOrZero private Long parentId;

    /** 分类名称。 */
    @NotBlank
    @Size(max = 50)
    private String categoryName;

    /** 展示排序。 */
    @NotNull @PositiveOrZero private Integer sort;

    /** 图片完整访问URL；沿用OssKey字段名，不再保存对象Key。空值表示未设置图片。 */
    @Size(max = 1024, message = "图片地址长度不能超过1024位")
    @org.hibernate.validator.constraints.URL(protocol = "https", message = "图片地址必须为完整HTTPS URL")
    private String iconOssKey;

    /** 是否启用：0否、1是。 */
    @NotNull private Integer enabled;
}
