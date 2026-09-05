package com.mallx.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import lombok.Data;

/** 新增或更新品牌请求。 */
@Data
public class BrandRequest {
    /** 品牌名称。 */
    @NotBlank
    @Size(max = 80)
    private String brandName;

    /** 图片完整访问URL；沿用OssKey字段名，不再保存对象Key。空值表示未设置图片。 */
    @Size(max = 1024, message = "图片地址长度不能超过1024位")
    @org.hibernate.validator.constraints.URL(protocol = "https", message = "图片地址必须为完整HTTPS URL")
    private String logoOssKey;

    /** 品牌介绍。 */
    @Size(max = 500)
    private String description;

    /** 展示排序。 */
    @NotNull @PositiveOrZero private Integer sort;

    /** 是否启用：0否、1是。 */
    @NotNull private Integer enabled;
}
