package com.mallx.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallx.product.entity.ProductSku;
import org.apache.ibatis.annotations.Mapper;

/** SKU数据访问接口。 */
@Mapper
public interface ProductSkuMapper extends BaseMapper<ProductSku> {
}
