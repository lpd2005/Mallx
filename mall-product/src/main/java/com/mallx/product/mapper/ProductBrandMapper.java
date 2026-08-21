package com.mallx.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallx.product.entity.ProductBrand;
import org.apache.ibatis.annotations.Mapper;

/** 商品品牌数据访问接口。 */
@Mapper
public interface ProductBrandMapper extends BaseMapper<ProductBrand> {
}
