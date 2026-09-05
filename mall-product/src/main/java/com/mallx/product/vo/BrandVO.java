package com.mallx.product.vo;

import lombok.Builder;
import lombok.Data;

/** 前台品牌信息。 */
@Data
@Builder
public class BrandVO {
    /** 品牌ID。 */
    private Long id;

    /** 品牌名称。 */
    private String brandName;

    /** 图片完整访问URL；沿用OssKey字段名，不再保存对象Key。空值表示未设置图片。 */
    private String logoOssKey;

    /** 品牌介绍。 */
    private String description;
}
