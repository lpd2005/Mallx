package com.mallx.product.service.impl;

import com.mallx.common.exception.BusinessException;
import com.mallx.product.dto.SkuRequest;
import com.mallx.product.entity.ProductSku;
import com.mallx.product.enums.ProductStatus;
import com.mallx.product.mapper.ProductSkuMapper;
import com.mallx.product.service.ProductSpuService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** SKU关键字段业务校验测试。 */
@ExtendWith(MockitoExtension.class)
class ProductSkuServiceImplTest {
    @Mock private ProductSkuMapper skuMapper;
    @Mock private ProductSpuService spuService;

    @Test
    void createSkuShouldRejectMarketPriceBelowSalePrice() {
        ProductSkuServiceImpl service = new ProductSkuServiceImpl(spuService, skuMapper);
        SkuRequest request = request();
        request.setSalePrice(new BigDecimal("100.00"));
        request.setMarketPrice(new BigDecimal("99.00"));
        assertThrows(BusinessException.class, () -> service.createSku(request));
    }

    @Test
    void createSkuShouldRejectNonObjectSpecJson() {
        ProductSkuServiceImpl service = new ProductSkuServiceImpl(spuService, skuMapper);
        SkuRequest request = request();
        request.setSpecJson("not-json");
        assertThrows(BusinessException.class, () -> service.createSku(request));
    }

    @Test
    void deleteSkuShouldPhysicallyDeleteDraftSku() {
        ProductSkuServiceImpl service = new ProductSkuServiceImpl(spuService, skuMapper);
        ReflectionTestUtils.setField(service, "baseMapper", skuMapper);
        ProductSku sku = new ProductSku();
        sku.setId(1L);
        sku.setStatus(ProductStatus.DRAFT.getValue());
        when(skuMapper.selectById(1L)).thenReturn(sku);
        when(skuMapper.deleteById(1L)).thenReturn(1);

        service.deleteSku(1L);

        verify(skuMapper).deleteById(1L);
    }

    @Test
    void deleteSkuShouldRejectSkuThatHasBeenPublished() {
        ProductSkuServiceImpl service = new ProductSkuServiceImpl(spuService, skuMapper);
        ReflectionTestUtils.setField(service, "baseMapper", skuMapper);
        ProductSku sku = new ProductSku();
        sku.setId(1L);
        sku.setStatus(ProductStatus.OFF_SHELF.getValue());
        when(skuMapper.selectById(1L)).thenReturn(sku);

        assertThrows(BusinessException.class, () -> service.deleteSku(1L));
    }

    private SkuRequest request() {
        SkuRequest request = new SkuRequest();
        request.setSpuId(1L);
        request.setSkuCode("SKU-001");
        request.setSkuName("测试SKU");
        request.setSpecJson("{\"颜色\":\"黑色\"}");
        request.setSalePrice(new BigDecimal("100.00"));
        return request;
    }
}
