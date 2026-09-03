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
    private String logoOssKey;
    private String description;
    private Integer sort;
    private Integer enabled;
}
