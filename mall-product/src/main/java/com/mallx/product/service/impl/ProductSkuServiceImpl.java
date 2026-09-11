package com.mallx.product.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;

import com.alicp.jetcache.anno.CachePenetrationProtect;
import com.alicp.jetcache.anno.CacheInvalidate;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mallx.common.enums.ResultCode;
import com.mallx.common.exception.BusinessException;
import com.mallx.product.cache.ProductCacheInvalidator;
import com.mallx.product.cache.ProductCacheDefinition;
import com.mallx.product.dto.SkuRequest;
import com.mallx.product.entity.ProductSku;
import com.mallx.product.entity.ProductSpu;
import com.mallx.product.enums.ProductStatus;
import com.mallx.product.mapper.ProductSkuMapper;
import com.mallx.product.service.ProductSkuService;
import com.mallx.product.service.ProductSearchEventRecorder;
import com.mallx.product.service.ProductSpuService;
import com.mallx.product.vo.CartProductVO;
import com.mallx.product.vo.SkuVO;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/** SKU业务实现。 */
@Service
@RequiredArgsConstructor
public class ProductSkuServiceImpl extends ServiceImpl<ProductSkuMapper, ProductSku>
        implements ProductSkuService {
    private final ProductSpuService spuService;
    private final ProductSkuMapper productSkuMapper;
    private final ProductCacheInvalidator cacheInvalidator;
    private final ProductSearchEventRecorder searchEventRecorder;
    private final RBloomFilter<Long> existingSkuIdBloomFilter;

    /** 创建初始状态为草稿的SKU。 */
    @Override
    @Transactional
    public ProductSku createSku(SkuRequest request) {
        spuService.requireSpu(request.getSpuId());
        validateRequest(null, request);
        ProductSku sku = new ProductSku();
        copy(request, sku);
        sku.setStatus(ProductStatus.DRAFT.getValue());
        save(sku);
        existingSkuIdBloomFilter.add(sku.getId());
        searchEventRecorder.record(sku.getSpuId());
        return sku;
    }

    /** 更新SKU；价格变化后同步刷新相关SPU价格区间。 */
    @Override
    @Transactional
    @CacheInvalidate(name = ProductCacheDefinition.Name.SKU_BY_ID, key = ProductCacheDefinition.Key.SKU_ID)
    @CacheInvalidate(name = ProductCacheDefinition.Name.SPU_BY_ID, key = ProductCacheDefinition.Key.REQUEST_SPU_ID)
    @CacheInvalidate(name = ProductCacheDefinition.Name.SKUS_BY_SPU_ID, key = ProductCacheDefinition.Key.REQUEST_SPU_ID)
    @CacheInvalidate(name = ProductCacheDefinition.Name.HOME_PAGE, key = ProductCacheDefinition.Key.DEFAULT_HOME_PAGE)
    public ProductSku updateSku(Long skuId, SkuRequest request) {
        ProductSku sku = requireSku(skuId);
        spuService.requireSpu(request.getSpuId());
        Long oldSpuId = sku.getSpuId();
        validateRequest(skuId, request);
        copy(request, sku);
        updateById(sku);
        refreshPriceRange(oldSpuId);
        if (!oldSpuId.equals(request.getSpuId())) refreshPriceRange(request.getSpuId());
        if (!oldSpuId.equals(request.getSpuId())) {
            cacheInvalidator.invalidateSpuAndSkuListBySpuIds(List.of(oldSpuId));
        }
        searchEventRecorder.recordAll(List.of(oldSpuId, request.getSpuId()));
        return sku;
    }

    /** 仅允许物理删除从未上架的草稿SKU。 */
    @Override
    @Transactional
    @CacheInvalidate(name = ProductCacheDefinition.Name.SKU_BY_ID, key = ProductCacheDefinition.Key.SKU_ID)
    @CacheInvalidate(name = ProductCacheDefinition.Name.HOME_PAGE, key = ProductCacheDefinition.Key.DEFAULT_HOME_PAGE)
    public void deleteSku(Long skuId) {
        ProductSku sku = requireSku(skuId);
        if (!ProductStatus.DRAFT.matches(sku.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "只有草稿状态的SKU可以删除");
        }
        if (!removeById(skuId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "SKU不存在");
        }
        cacheInvalidator.invalidateSpuAndSkuListBySpuIds(List.of(sku.getSpuId()));
        searchEventRecorder.record(sku.getSpuId());
    }

    /** 修改SKU状态并刷新SPU价格区间。 */
    @Override
    @Transactional
    @CacheInvalidate(name = ProductCacheDefinition.Name.SKU_BY_ID, key = ProductCacheDefinition.Key.SKU_ID)
    @CacheInvalidate(name = ProductCacheDefinition.Name.HOME_PAGE, key = ProductCacheDefinition.Key.DEFAULT_HOME_PAGE)
    public void changeStatus(Long skuId, Integer status) {
        if (!ProductStatus.isValid(status)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "SKU状态不正确");
        }
        ProductSku sku = requireSku(skuId);
        lambdaUpdate().eq(ProductSku::getId, skuId).set(ProductSku::getStatus, status).update();
        refreshPriceRange(sku.getSpuId());
        cacheInvalidator.invalidateSpuAndSkuListBySpuIds(List.of(sku.getSpuId()));
        searchEventRecorder.record(sku.getSpuId());
    }

    /** 获取必须存在的SKU，库存初始化会调用此方法校验商品。 */
    @Override
    public ProductSku requireSku(Long skuId) {
        ProductSku sku = getById(skuId);
        if (sku == null) throw new BusinessException(ResultCode.NOT_FOUND, "SKU不存在");
        return sku;
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public ProductSku lockSku(Long skuId) {
        ProductSku sku = productSkuMapper.selectByIdForUpdate(skuId);
        if (sku == null) throw new BusinessException(ResultCode.NOT_FOUND, "SKU不存在");
        return sku;
    }

    /** 查询指定已上架SPU的可售SKU。 */
    @Override
    @Cached(name = ProductCacheDefinition.Name.SKUS_BY_SPU_ID, key = ProductCacheDefinition.Key.SPU_ID,
            expire = 300, localExpire = 20, cacheType = CacheType.BOTH, syncLocal = true)
    @CachePenetrationProtect(timeout = 5)
    public List<SkuVO> listBySpu(Long spuId) {
        spuService.getSpu(spuId);
        return new ArrayList<>(
                lambdaQuery()
                        .eq(ProductSku::getSpuId, spuId)
                        .eq(ProductSku::getStatus, ProductStatus.ON_SHELF.getValue())
                        .orderByAsc(ProductSku::getSalePrice)
                        .list()
                        .stream()
                        .map(this::toVO)
                        .toList());
    }

    /** 查询前台可见SKU详情。 */
    @Override
    @Cached(name = ProductCacheDefinition.Name.SKU_BY_ID, key = ProductCacheDefinition.Key.SKU_ID,
            expire = 480, localExpire = 25, cacheType = CacheType.BOTH, syncLocal = true)
    @CachePenetrationProtect(timeout = 5)
    public SkuVO getSku(Long skuId) {
        if (skuId == null || !existingSkuIdBloomFilter.contains(skuId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "SKU不存在或未上架");
        }
        ProductSku sku = getById(skuId);
        if (sku == null || !ProductStatus.ON_SHELF.matches(sku.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "SKU不存在或未上架");
        }
        ProductSpu parent = spuService.getById(sku.getSpuId());
        if (parent == null || !ProductStatus.ON_SHELF.matches(parent.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "SKU不存在或未上架");
        }
        return toVO(sku);
    }

    /** 一次查询购物车中多个SKU及其所属SPU的当前信息，避免N+1查询。 */
    @Override
    public List<CartProductVO> listCartProducts(Collection<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return List.of();
        }
        List<Long> validSkuIds =
                skuIds.stream().filter(skuId -> skuId != null && skuId > 0).distinct().toList();
        if (validSkuIds.isEmpty()) {
            return List.of();
        }
        return productSkuMapper.selectCartProducts(validSkuIds);
    }

    private void validateRequest(Long excludedId, SkuRequest request) {
        if (request.getMarketPrice() != null
                && request.getMarketPrice().compareTo(request.getSalePrice()) < 0)
            throw new BusinessException(ResultCode.BAD_REQUEST, "划线价不能低于销售价");
        try {
            JSONUtil.parseObj(request.getSpecJson());
        } catch (Exception exception) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "SKU规格必须是JSON对象");
        }
        long count =
                lambdaQuery()
                        .eq(ProductSku::getSkuCode, request.getSkuCode().trim())
                        .ne(excludedId != null, ProductSku::getId, excludedId)
                        .count();
        if (count > 0) throw new BusinessException(ResultCode.BAD_REQUEST, "SKU编码已存在");
    }

    private void copy(SkuRequest request, ProductSku sku) {
        BeanUtil.copyProperties(request, sku);
        sku.setSkuCode(request.getSkuCode().trim());
        sku.setSkuName(request.getSkuName().trim());
        sku.setSpecJson(JSONUtil.parseObj(request.getSpecJson()).toString());
    }

    /** 汇总当前SPU已上架SKU的价格区间。 */
    private void refreshPriceRange(Long spuId) {
        List<BigDecimal> prices =
                lambdaQuery()
                        .eq(ProductSku::getSpuId, spuId)
                        .eq(ProductSku::getStatus, ProductStatus.ON_SHELF.getValue())
                        .select(ProductSku::getSalePrice)
                        .list()
                        .stream()
                        .map(ProductSku::getSalePrice)
                        .sorted(Comparator.naturalOrder())
                        .toList();
        BigDecimal min = prices.isEmpty() ? null : prices.get(0);
        BigDecimal max = prices.isEmpty() ? null : prices.get(prices.size() - 1);
        spuService
                .lambdaUpdate()
                .eq(ProductSpu::getId, spuId)
                .set(ProductSpu::getMinPrice, min)
                .set(ProductSpu::getMaxPrice, max)
                .update();
    }

    private SkuVO toVO(ProductSku sku) {
        return SkuVO.builder()
                .id(sku.getId())
                .spuId(sku.getSpuId())
                .skuCode(sku.getSkuCode())
                .skuName(sku.getSkuName())
                .specJson(sku.getSpecJson())
                .imageOssKey(sku.getImageOssKey())
                .salePrice(sku.getSalePrice())
                .marketPrice(sku.getMarketPrice())
                .status(sku.getStatus())
                .build();
    }
}
