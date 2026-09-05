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

    /** 图片完整访问URL；沿用OssKey字段名，不再保存对象Key。空值表示未设置图片。 */
    private String iconOssKey;

    private Integer enabled;
}
