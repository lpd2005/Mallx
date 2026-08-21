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
    /** Logo OSS对象Key。 */
    private String logoOssKey;
    /** 品牌介绍。 */
    private String description;
}
