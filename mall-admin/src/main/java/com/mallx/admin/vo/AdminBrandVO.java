package com.mallx.admin.vo;

import lombok.*;

/** 后台Brand视图，保留状态字段，可展示草稿/禁用数据；字段含义与ProductBrand一致。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminBrandVO {
    private Long id;
    private String brandName;

    /** 图片完整访问URL；沿用OssKey字段名，不再保存对象Key。空值表示未设置图片。 */
    private String logoOssKey;

    private String description;
    private Integer sort;
    private Integer enabled;
}
