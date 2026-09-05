package com.mallx.admin.vo;

import lombok.*;

import java.math.BigDecimal;

/** 后台Sku视图，保留状态字段，可展示草稿/禁用数据；字段含义与ProductSku一致。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSkuVO {
    private Long id;
    private Long spuId;
    private String skuCode;
    private String skuName;
    private String specJson;

    /** 图片完整访问URL；沿用OssKey字段名，不再保存对象Key。空值表示未设置图片。 */
    private String imageOssKey;

    private BigDecimal salePrice;
    private BigDecimal marketPrice;
    private Integer status;
}
