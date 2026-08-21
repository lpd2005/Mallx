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
    /** 品牌Logo OSS对象Key。 */
    @Size(max = 255)
    private String logoOssKey;
    /** 品牌介绍。 */
    @Size(max = 500)
    private String description;
    /** 展示排序。 */
    @NotNull
    @PositiveOrZero
    private Integer sort;
    /** 是否启用：0否、1是。 */
    @NotNull
    private Integer enabled;
}
