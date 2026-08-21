package com.mallx.product.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.mallx.product.dto.BrandRequest;
import com.mallx.product.entity.ProductBrand;
import com.mallx.product.vo.BrandVO;

import java.util.List;

/** 商品品牌业务服务。 */
public interface ProductBrandService extends IService<ProductBrand> {
    /** 按排序值查询前台可见品牌。 */
    List<BrandVO> listEnabledBrands();
    /** 创建品牌。 */
    ProductBrand createBrand(BrandRequest request);
    /** 更新指定品牌。 */
    ProductBrand updateBrand(Long brandId, BrandRequest request);
    /** 物理删除未关联SPU的品牌。 */
    void deleteBrand(Long brandId);
    /** 获取必须存在且已启用的品牌。 */
    ProductBrand requireEnabledBrand(Long brandId);
}
