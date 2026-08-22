package com.mallx.cart.service.impl;

import com.mallx.cart.vo.CartVO;
import com.mallx.common.exception.BusinessException;
import com.mallx.inventory.service.InventoryStockService;
import com.mallx.inventory.vo.InventoryStockVO;
import com.mallx.product.enums.ProductStatus;
import com.mallx.product.service.ProductSkuService;
import com.mallx.product.vo.CartProductVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/** 购物车实时价格、库存校验和用户Key隔离测试。 */
@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {
    private static final Long USER_ID = 1001L;
    private static final Long SKU_ID = 2001L;

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private HashOperations<String, Object, Object> hashOperations;
    @Mock private ProductSkuService productSkuService;
    @Mock private InventoryStockService inventoryStockService;

    @Test
    void getCartShouldAlwaysUseCurrentProductPrice() {
        CartServiceImpl service = serviceWithCartItem(2, true);
        when(productSkuService.listCartProducts(anyCollection()))
                .thenReturn(List.of(product("100.00")), List.of(product("80.00")));
        when(inventoryStockService.listStocks(anyCollection())).thenReturn(List.of(stock(10)));

        CartVO beforePriceChange = service.getCart(USER_ID);
        CartVO afterPriceChange = service.getCart(USER_ID);

        assertEquals(new BigDecimal("200.00"), beforePriceChange.getSelectedAmount());
        assertEquals(new BigDecimal("160.00"), afterPriceChange.getSelectedAmount());
        assertEquals(new BigDecimal("80.00"), afterPriceChange.getItems().get(0).getSalePrice());
        verify(hashOperations, times(2)).entries("mall:cart:{1001}");
    }

    @Test
    void checkoutShouldRejectQuantityGreaterThanCurrentStock() {
        CartServiceImpl service = serviceWithCartItem(3, true);
        when(productSkuService.listCartProducts(anyCollection())).thenReturn(List.of(product("80.00")));
        when(inventoryStockService.listStocks(anyCollection())).thenReturn(List.of(stock(2)));

        assertThrows(BusinessException.class, () -> service.checkout(USER_ID));
    }

    @Test
    void getCartShouldKeepButMarkOffShelfProductAsUnavailable() {
        CartServiceImpl service = serviceWithCartItem(1, true);
        CartProductVO product = product("80.00");
        product.setSkuStatus(ProductStatus.OFF_SHELF.getValue());
        when(productSkuService.listCartProducts(anyCollection())).thenReturn(List.of(product));
        when(inventoryStockService.listStocks(anyCollection())).thenReturn(List.of(stock(10)));

        CartVO result = service.getCart(USER_ID);

        assertEquals(false, result.getItems().get(0).getPurchasable());
        assertEquals("SKU已下架", result.getItems().get(0).getInvalidReason());
        assertEquals(BigDecimal.ZERO, result.getSelectedAmount());
    }

    @Test
    void getCartShouldReturnEmptySummaryWhenRedisHasNoItems() {
        when(redisTemplate.<Object, Object>opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries("mall:cart:{1001}")).thenReturn(Map.of());
        CartServiceImpl service = new CartServiceImpl(redisTemplate, productSkuService, inventoryStockService);

        CartVO result = service.getCart(USER_ID);

        assertEquals(0, result.getItemKinds());
        assertEquals(0, result.getTotalQuantity());
        assertEquals(BigDecimal.ZERO, result.getSelectedAmount());
    }

    private CartServiceImpl serviceWithCartItem(int quantity, boolean selected) {
        when(redisTemplate.<Object, Object>opsForHash()).thenReturn(hashOperations);
        String json = "{\"quantity\":" + quantity + ",\"selected\":" + selected + ",\"addedAt\":1000}";
        when(hashOperations.entries("mall:cart:{1001}")).thenReturn(Map.of(SKU_ID.toString(), json));
        return new CartServiceImpl(redisTemplate, productSkuService, inventoryStockService);
    }

    private CartProductVO product(String salePrice) {
        return CartProductVO.builder()
                .skuId(SKU_ID)
                .spuId(3001L)
                .productName("测试商品")
                .skuName("黑色")
                .specJson("{\"颜色\":\"黑色\"}")
                .salePrice(new BigDecimal(salePrice))
                .marketPrice(new BigDecimal("120.00"))
                .spuStatus(ProductStatus.ON_SHELF.getValue())
                .skuStatus(ProductStatus.ON_SHELF.getValue())
                .build();
    }

    private InventoryStockVO stock(int availableStock) {
        return InventoryStockVO.builder()
                .skuId(SKU_ID)
                .availableStock(availableStock)
                .lockedStock(0)
                .soldStock(0)
                .outOfStock(availableStock == 0)
                .build();
    }
}
