package com.mallx.admin.vo;

import lombok.*;

/** 后台Category视图，保留状态字段，可展示草稿/禁用数据；字段含义与ProductCategory一致。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCategoryVO {
    private Long id;
    private Long parentId;
    private String categoryName;
    private Integer categoryLevel;
    private Integer sort;
    private String iconOssKey;
    private Integer enabled;
}
