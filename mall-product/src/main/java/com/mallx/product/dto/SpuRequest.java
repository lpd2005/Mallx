package com.mallx.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

/** 新增或更新SPU请求。 */
@Data
public class SpuRequest {
    /** 所属分类ID。 */
    @NotNull private Long categoryId;

    /** 所属品牌ID。 */
    @NotNull private Long brandId;

    /** 商品名称。 */
    @NotBlank
    @Size(max = 120)
    private String productName;

    /** 商品副标题。 */
    @Size(max = 255)
    private String subtitle;

    /** 图片完整访问URL；沿用OssKey字段名，不再保存对象Key。空值表示未设置图片。 */
    @Size(max = 1024, message = "图片地址长度不能超过1024位")
    @org.hibernate.validator.constraints.URL(protocol = "https", message = "图片地址必须为完整HTTPS URL")
    private String mainImageOssKey;

    /** 商品描述信息。 */
    @Size(max = 1000)
    private String description;
}
