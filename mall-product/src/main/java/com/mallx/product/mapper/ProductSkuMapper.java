package com.mallx.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallx.product.entity.ProductSku;
import com.mallx.product.vo.CartProductVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/** SKU数据访问接口。 */
@Mapper
public interface ProductSkuMapper extends BaseMapper<ProductSku> {
    /** 固定单行SQL无需XML；锁持续到外层事务结束。 */
    @Select("SELECT * FROM product_sku WHERE id = #{skuId} FOR UPDATE")
    ProductSku selectByIdForUpdate(@Param("skuId") Long skuId);
    /** 批量关联SPU与SKU，返回购物车展示所需的当前商品信息。 */
    List<CartProductVO> selectCartProducts(@Param("skuIds") Collection<Long> skuIds);
}
