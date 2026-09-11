package com.mallx.search.service.impl;

import com.mallx.inventory.service.InventoryStockService;
import com.mallx.inventory.vo.InventoryStockVO;
import com.mallx.product.entity.ProductBrand;
import com.mallx.product.entity.ProductCategory;
import com.mallx.product.entity.ProductSku;
import com.mallx.product.entity.ProductSpu;
import com.mallx.product.enums.ProductStatus;
import com.mallx.product.service.ProductBrandService;
import com.mallx.product.service.ProductCategoryService;
import com.mallx.product.service.ProductSkuService;
import com.mallx.product.service.ProductSpuService;
import com.mallx.search.document.ProductSearchDocument;
import com.mallx.search.document.ProductSearchSkuDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 批量读取商品、分类、品牌和库存，避免全量建索引时产生 N+1 查询。
 * MySQL 数据 → ES 搜索文档
 *
 */
@Component
@RequiredArgsConstructor
public class ProductSearchDocumentAssembler {
    private final ProductSpuService spuService;
    private final ProductSkuService skuService;
    private final ProductBrandService brandService;
    private final ProductCategoryService categoryService;
    private final InventoryStockService inventoryStockService;

    /**
     * 批量聚合指定 SPU 的搜索文档。
     *
     * <p>返回 Map 是为了让单商品同步能够判断目标 SPU 是否仍应存在于索引中：
     * Map 中不存在代表商品已下架、已删除或没有在售 SKU，应删除旧 ES 文档。</p>
     */
    public Map<Long, ProductSearchDocument> build(Collection<Long> spuIds) {
        List<Long> ids = spuIds == null ? List.of() : spuIds.stream()
                .filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        // 搜索索引只收录前台可见 SPU，草稿和下架商品会被排除。
        List<ProductSpu> spus = spuService.listByIds(ids).stream()
                .filter(spu -> ProductStatus.ON_SHELF.matches(spu.getStatus()))
                .toList();
        if (spus.isEmpty()) {
            return Map.of();
        }
        List<Long> visibleSpuIds = spus.stream().map(ProductSpu::getId).toList();
        // 一次查询当前批次的所有在售 SKU，避免逐 SPU 查询形成 N+1。
        List<ProductSku> skus = skuService.lambdaQuery()
                .in(ProductSku::getSpuId, visibleSpuIds)
                .eq(ProductSku::getStatus, ProductStatus.ON_SHELF.getValue())
                .list();
        Map<Long, List<ProductSku>> skusBySpu = skus.stream()
                .collect(Collectors.groupingBy(ProductSku::getSpuId));
        // 库存属于 mall-inventory，批量读取后按 SKU ID 建索引便于组装。
        Map<Long, InventoryStockVO> stocks = inventoryStockService
                .listStocks(skus.stream().map(ProductSku::getId).toList())
                .stream()
                .collect(Collectors.toMap(InventoryStockVO::getSkuId, Function.identity()));
        Map<Long, ProductBrand> brands = brandService
                .listByIds(spus.stream().map(ProductSpu::getBrandId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(ProductBrand::getId, Function.identity()));
        // 分类名称变更也会影响搜索召回，因此文档中保存完整分类路径。
        Map<Long, ProductCategory> categories = loadCategoryPaths(
                spus.stream().map(ProductSpu::getCategoryId).collect(Collectors.toSet()));

        Map<Long, ProductSearchDocument> documents = new HashMap<>();
        for (ProductSpu spu : spus) {
            List<ProductSku> sellableSkus = skusBySpu.getOrDefault(spu.getId(), List.of());
            if (sellableSkus.isEmpty()) {
                continue;
            }
            ProductBrand brand = brands.get(spu.getBrandId());
            List<ProductCategory> categoryPath = categoryPath(spu.getCategoryId(), categories);
            List<ProductSearchSkuDocument> skuDocuments = sellableSkus.stream()
                    .map(sku -> toSkuDocument(sku, stocks.get(sku.getId())))
                    .toList();
            // 价格、库存和销量在建文档时预聚合，查询时无需脚本计算。
            BigDecimal minPrice = sellableSkus.stream().map(ProductSku::getSalePrice)
                    .min(Comparator.naturalOrder()).orElse(null);
            BigDecimal maxPrice = sellableSkus.stream().map(ProductSku::getSalePrice)
                    .max(Comparator.naturalOrder()).orElse(null);
            long soldCount = skuDocuments.stream().map(ProductSearchSkuDocument::getSoldStock)
                    .filter(Objects::nonNull).mapToLong(Integer::longValue).sum();
            boolean hasStock = skuDocuments.stream()
                    .anyMatch(sku -> sku.getAvailableStock() != null && sku.getAvailableStock() > 0);
            documents.put(spu.getId(), ProductSearchDocument.builder()
                    .spuId(spu.getId())
                    .productName(spu.getProductName())
                    .subtitle(spu.getSubtitle())
                    .description(spu.getDescription())
                    .mainImageUrl(spu.getMainImageOssKey())
                    .brandId(spu.getBrandId())
                    .brandName(brand == null ? null : brand.getBrandName())
                    .categoryId(spu.getCategoryId())
                    .categoryPathIds(categoryPath.stream().map(ProductCategory::getId).toList())
                    .categoryPathNames(categoryPath.stream().map(ProductCategory::getCategoryName).toList())
                    .minPrice(minPrice)
                    .maxPrice(maxPrice)
                    .hasStock(hasStock)
                    .soldCount(soldCount)
                    .updatedAt(spu.getUpdatedAt())
                    .skus(skuDocuments)
                    .build());
        }
        return documents;
    }

    /** 库存尚未初始化时按 0 处理，避免不存在的库存被误判为可售。 */
    private ProductSearchSkuDocument toSkuDocument(ProductSku sku, InventoryStockVO stock) {
        return ProductSearchSkuDocument.builder()
                .skuId(sku.getId())
                .skuName(sku.getSkuName())
                .specText(sku.getSpecJson())
                .salePrice(sku.getSalePrice())
                .availableStock(stock == null ? 0 : stock.getAvailableStock())
                .soldStock(stock == null ? 0 : stock.getSoldStock())
                .build();
    }

    /**
     * 从叶子分类逐层向上批量加载父节点。当前商品模型最多三级，
     * 因此最多循环三次，不会对每个 SPU 单独递归查库。
     */
    private Map<Long, ProductCategory> loadCategoryPaths(Set<Long> leafIds) {
        Map<Long, ProductCategory> result = new HashMap<>();
        Set<Long> currentIds = new LinkedHashSet<>(leafIds);
        for (int level = 0; level < 3 && !currentIds.isEmpty(); level++) {
            List<ProductCategory> current = categoryService.listByIds(currentIds);
            current.forEach(category -> result.put(category.getId(), category));
            currentIds = current.stream().map(ProductCategory::getParentId)
                    .filter(parentId -> parentId != null && parentId > 0 && !result.containsKey(parentId))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return result;
    }

    /** 把“叶子到根”的追溯结果反转为“根到叶子”的展示和筛选路径。 */
    private List<ProductCategory> categoryPath(Long categoryId, Map<Long, ProductCategory> categories) {
        List<ProductCategory> reversed = new ArrayList<>();
        Long currentId = categoryId;
        while (currentId != null && currentId > 0 && reversed.size() < 3) {
            ProductCategory category = categories.get(currentId);
            if (category == null) {
                break;
            }
            reversed.add(category);
            currentId = category.getParentId();
        }
        List<ProductCategory> result = new ArrayList<>();
        for (int index = reversed.size() - 1; index >= 0; index--) {
            result.add(reversed.get(index));
        }
        return result;
    }
}
