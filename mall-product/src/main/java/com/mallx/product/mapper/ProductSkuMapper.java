package com.mallx.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallx.product.entity.ProductSku;
import com.mallx.product.vo.CartProductVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/** SKU数据访问接口。 */
@Mapper
public interface ProductSkuMapper extends BaseMapper<ProductSku> {
    /** 批量关联SPU与SKU，返回购物车展示所需的当前商品信息。 */
    List<CartProductVO> selectCartProducts(@Param("skuIds") Collection<Long> skuIds);
}
