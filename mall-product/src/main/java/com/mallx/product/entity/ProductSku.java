package com.mallx.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 可销售、可定价并由库存中心独立管理库存的最小商品单元。 */
@Data
@TableName("product_sku")
public class ProductSku {
    /** SKU主键，也是库存中心的业务关联键。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    /** 所属SPU。 */
    private Long spuId;
    /** 全局唯一业务SKU编码。 */
    private String skuCode;
    /** SKU展示名称。 */
    private String skuName;
    /** JSON字符串，数据库使用JSON类型校验格式。 */
    private String specJson;
    /** SKU图片OSS对象Key。 */
    private String imageOssKey;
    /** 实际销售价。 */
    private BigDecimal salePrice;
    /** 划线价，不得低于销售价。 */
    private BigDecimal marketPrice;
    /** 状态：0草稿、1上架、2下架。 */
    private Integer status;
    /** 创建时间。 */
    private LocalDateTime createdAt;
    /** 最后更新时间。 */
    private LocalDateTime updatedAt;
}
