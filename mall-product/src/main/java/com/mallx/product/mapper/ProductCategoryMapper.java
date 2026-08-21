package com.mallx.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallx.product.entity.ProductCategory;
import org.apache.ibatis.annotations.Mapper;

/** 商品分类数据访问接口，普通CRUD由MyBatis-Plus提供。 */
@Mapper
public interface ProductCategoryMapper extends BaseMapper<ProductCategory> {
}
