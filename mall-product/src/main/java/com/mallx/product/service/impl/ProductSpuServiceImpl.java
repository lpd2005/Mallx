package com.mallx.product.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mallx.common.enums.ResultCode;
import com.mallx.common.exception.BusinessException;
import com.mallx.product.dto.SpuRequest;
import com.mallx.product.entity.ProductSpu;
import com.mallx.product.entity.ProductSku;
import com.mallx.product.enums.ProductStatus;
import com.mallx.product.mapper.ProductSkuMapper;
import com.mallx.product.mapper.ProductSpuMapper;
import com.mallx.product.service.ProductBrandService;
import com.mallx.product.service.ProductCategoryService;
import com.mallx.product.service.ProductSpuService;
import com.mallx.product.vo.SpuVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** SPU业务实现。 */
@Service
@RequiredArgsConstructor
public class ProductSpuServiceImpl extends ServiceImpl<ProductSpuMapper, ProductSpu>
        implements ProductSpuService {
    private final ProductCategoryService categoryService;
    private final ProductBrandService brandService;
    private final ProductSkuMapper productSkuMapper;

    /** 创建初始状态为草稿的SPU。 */
    @Override
    @Transactional
    public ProductSpu createSpu(SpuRequest request) {
        validateRelations(request);
        ProductSpu spu = new ProductSpu();
        copy(request, spu);
        spu.setStatus(ProductStatus.DRAFT.getValue());
        save(spu);
        return spu;
    }

    /** 更新SPU共享信息。 */
    @Override
    @Transactional
    public ProductSpu updateSpu(Long spuId, SpuRequest request) {
        ProductSpu spu = requireSpu(spuId);
        validateRelations(request);
        copy(request, spu);
        updateById(spu);
        return spu;
    }

    /** 仅允许物理删除没有SKU的草稿SPU。 */
    @Override
    @Transactional
    public void deleteSpu(Long spuId) {
        ProductSpu spu = requireSpu(spuId);
        if (!ProductStatus.DRAFT.matches(spu.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "只有草稿状态的SPU可以删除");
        }
        Long skuCount = productSkuMapper.selectCount(Wrappers.<ProductSku>lambdaQuery()
                .eq(ProductSku::getSpuId, spuId));
        if (skuCount > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "SPU下存在SKU，不能删除");
        }
        if (!removeById(spuId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "SPU不存在");
        }
    }

    /** 修改SPU状态；没有已上架SKU时不能上架。 */
    @Override
    @Transactional
    public void changeStatus(Long spuId, Integer status) {
        if (!ProductStatus.isValid(status)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "商品状态不正确");
        }
        ProductSpu spu = requireSpu(spuId);
        if (ProductStatus.ON_SHELF.getValue() == status && spu.getMinPrice() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "至少需要一个已上架SKU才能上架SPU");
        }
        lambdaUpdate().eq(ProductSpu::getId, spuId)
                .set(ProductSpu::getStatus, status).update();
    }

    /** 查询必须存在的有效SPU记录。 */
    @Override
    public ProductSpu requireSpu(Long spuId) {
        ProductSpu spu = getById(spuId);
        if (spu == null) throw new BusinessException(ResultCode.NOT_FOUND, "SPU不存在");
        return spu;
    }

    /** 查询前台可见SPU。 */
    @Override
    public SpuVO getSpu(Long spuId) {
        ProductSpu spu = requireSpu(spuId);
        if (!ProductStatus.ON_SHELF.matches(spu.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在或未上架");
        }
        return toVO(spu);
    }

    private void validateRelations(SpuRequest request) {
        categoryService.requireEnabledCategory(request.getCategoryId());
        brandService.requireEnabledBrand(request.getBrandId());
    }

    private void copy(SpuRequest request, ProductSpu spu) {
        BeanUtil.copyProperties(request, spu);
        spu.setProductName(request.getProductName().trim());
    }

    private SpuVO toVO(ProductSpu spu) {
        return SpuVO.builder()
                .id(spu.getId())
                .categoryId(spu.getCategoryId())
                .brandId(spu.getBrandId())
                .productName(spu.getProductName())
                .subtitle(spu.getSubtitle())
                .mainImageOssKey(spu.getMainImageOssKey())
                .description(spu.getDescription())
                .minPrice(spu.getMinPrice())
                .maxPrice(spu.getMaxPrice())
                .status(spu.getStatus())
                .build();
    }
}
