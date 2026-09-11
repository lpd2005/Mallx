package com.mallx.product.service.impl;

import cn.hutool.core.bean.BeanUtil;

import com.alicp.jetcache.anno.CachePenetrationProtect;
import com.alicp.jetcache.anno.CacheRefresh;
import com.alicp.jetcache.anno.CacheInvalidate;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mallx.common.enums.ResultCode;
import com.mallx.common.exception.BusinessException;
import com.mallx.product.cache.ProductCacheDefinition;
import com.mallx.product.dto.BrandRequest;
import com.mallx.product.entity.ProductBrand;
import com.mallx.product.entity.ProductSpu;
import com.mallx.product.mapper.ProductBrandMapper;
import com.mallx.product.mapper.ProductSpuMapper;
import com.mallx.product.service.ProductBrandService;
import com.mallx.product.service.ProductSearchEventRecorder;
import com.mallx.product.vo.BrandVO;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** 商品品牌业务实现。 */
@Service
@RequiredArgsConstructor
public class ProductBrandServiceImpl extends ServiceImpl<ProductBrandMapper, ProductBrand>
        implements ProductBrandService {
    private static final int ENABLED = 1;
    private final ProductSpuMapper productSpuMapper;
    private final ProductSearchEventRecorder searchEventRecorder;

    /** 查询前台可见品牌。 */
    @Override
    @Cached(name = ProductCacheDefinition.Name.ENABLED_BRANDS, key = ProductCacheDefinition.Key.ALL,
            expire = 1800, localExpire = 60,
            cacheType = CacheType.BOTH, syncLocal = true)
    @CacheRefresh(refresh = 600, stopRefreshAfterLastAccess = 1800, refreshLockTimeout = 10)
    @CachePenetrationProtect(timeout = 5)
    public List<BrandVO> listEnabledBrands() {
        return new ArrayList<>(
                lambdaQuery()
                        .eq(ProductBrand::getEnabled, ENABLED)
                        .orderByAsc(ProductBrand::getSort)
                        .list()
                        .stream()
                        .map(this::toVO)
                        .toList());
    }

    /** 创建品牌并校验名称唯一性。 */
    @Override
    @Transactional
    @CacheInvalidate(name = ProductCacheDefinition.Name.ENABLED_BRANDS, key = ProductCacheDefinition.Key.ALL)
    public ProductBrand createBrand(BrandRequest request) {
        ensureUnique(null, request.getBrandName());
        ProductBrand brand = new ProductBrand();
        copy(request, brand);
        save(brand);
        return brand;
    }

    /** 更新品牌并排除自身检查重名。 */
    @Override
    @Transactional
    @CacheInvalidate(name = ProductCacheDefinition.Name.ENABLED_BRANDS, key = ProductCacheDefinition.Key.ALL)
    public ProductBrand updateBrand(Long brandId, BrandRequest request) {
        ProductBrand brand = requireBrand(brandId);
        ensureUnique(brandId, request.getBrandName());
        copy(request, brand);
        updateById(brand);
        List<Long> affectedSpuIds =
                productSpuMapper
                        .selectList(
                                Wrappers.<ProductSpu>lambdaQuery()
                                        .select(ProductSpu::getId)
                                        .eq(ProductSpu::getBrandId, brandId))
                        .stream()
                        .map(ProductSpu::getId)
                        .toList();
        searchEventRecorder.recordAll(affectedSpuIds);
        return brand;
    }

    /** 物理删除没有关联商品的品牌。 */
    @Override
    @Transactional
    @CacheInvalidate(name = ProductCacheDefinition.Name.ENABLED_BRANDS, key = ProductCacheDefinition.Key.ALL)
    public void deleteBrand(Long brandId) {
        requireBrand(brandId);
        Long spuCount =
                productSpuMapper.selectCount(
                        Wrappers.<ProductSpu>lambdaQuery().eq(ProductSpu::getBrandId, brandId));
        if (spuCount > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "品牌已关联商品，不能删除");
        }
        if (!removeById(brandId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商品品牌不存在");
        }
    }

    /** 获取启用品牌，供SPU业务校验。 */
    @Override
    public ProductBrand requireEnabledBrand(Long brandId) {
        ProductBrand brand = requireBrand(brandId);
        if (!Integer.valueOf(ENABLED).equals(brand.getEnabled())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "商品品牌未启用");
        }
        return brand;
    }

    private ProductBrand requireBrand(Long id) {
        ProductBrand brand = getById(id);
        if (brand == null) throw new BusinessException(ResultCode.NOT_FOUND, "商品品牌不存在");
        return brand;
    }

    private void ensureUnique(Long excludedId, String name) {
        long count =
                lambdaQuery()
                        .eq(ProductBrand::getBrandName, name.trim())
                        .ne(excludedId != null, ProductBrand::getId, excludedId)
                        .count();
        if (count > 0) throw new BusinessException(ResultCode.BAD_REQUEST, "品牌名称已存在");
    }

    private void copy(BrandRequest request, ProductBrand brand) {
        if (request.getEnabled() == null
                || (request.getEnabled() != 0 && request.getEnabled() != 1))
            throw new BusinessException(ResultCode.BAD_REQUEST, "启用状态不正确");
        BeanUtil.copyProperties(request, brand);
        brand.setBrandName(request.getBrandName().trim());
    }

    private BrandVO toVO(ProductBrand brand) {
        return BrandVO.builder()
                .id(brand.getId())
                .brandName(brand.getBrandName())
                .logoOssKey(brand.getLogoOssKey())
                .description(brand.getDescription())
                .build();
    }
}
