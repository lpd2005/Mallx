package com.mallx.admin.vo;

import lombok.*;

import java.math.BigDecimal;

/** 后台Spu视图，保留状态字段，可展示草稿/禁用数据；字段含义与ProductSpu一致。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSpuVO {
    private Long id;
    private Long categoryId;
    private Long brandId;
    private String productName;
    private String subtitle;
    private String mainImageOssKey;
    private String description;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer status;
}
