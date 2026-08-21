package com.mallx.inventory.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.mallx.common.exception.BusinessException;
import com.mallx.inventory.entity.InventoryOperation;
import com.mallx.inventory.entity.InventoryStock;
import com.mallx.inventory.mapper.InventoryOperationMapper;
import com.mallx.inventory.mapper.InventoryStockMapper;
import com.mallx.inventory.service.InventoryOperationService;
import com.mallx.inventory.vo.InventoryStockVO;
import com.mallx.product.service.ProductSkuService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 库存锁定、扣减、释放与余额保护测试。 */
@ExtendWith(MockitoExtension.class)
class InventoryStockServiceImplTest {
    private static final Long SKU_ID = 2001L;

    @BeforeAll
    static void initializeMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, "stock-test"), InventoryStock.class);
    }

    @Mock private InventoryStockMapper stockMapper;
    @Mock private InventoryOperationMapper operationMapper;
    @Mock private InventoryOperationService operationService;
    @Mock private ProductSkuService skuService;
    private InventoryStockServiceImpl stockService;

    @BeforeEach
    void setUp() {
        stockService = new InventoryStockServiceImpl(stockMapper, operationMapper, operationService, skuService) {
            @Override public Class<InventoryStock> getEntityClass() { return InventoryStock.class; }
            @Override public Class<InventoryStockMapper> getMapperClass() { return InventoryStockMapper.class; }
        };
        ReflectionTestUtils.setField(stockService, "baseMapper", stockMapper);
    }

    @Test
    void stockShouldFlowFromAvailableToLockedThenSold() {
        when(operationMapper.insertIgnore(any(InventoryOperation.class))).thenReturn(1);
        when(stockMapper.update(any(), any())).thenReturn(1);
        when(operationService.updateById(any(InventoryOperation.class))).thenReturn(true);
        when(stockMapper.selectById(SKU_ID)).thenReturn(
                stock(10, 0, 0), stock(7, 3, 0),
                stock(7, 3, 0), stock(7, 0, 3));

        InventoryStockVO locked = stockService.lockStock(SKU_ID, 3, "ORDER-1001");
        InventoryStockVO deducted = stockService.deductStock(SKU_ID, 3, "ORDER-1001");

        assertEquals(7, locked.getAvailableStock());
        assertEquals(3, locked.getLockedStock());
        assertEquals(0, deducted.getLockedStock());
        assertEquals(3, deducted.getSoldStock());
    }

    @Test
    void increaseShouldInitializeSkuStockAndAddAvailableQuantity() {
        when(stockMapper.insertIfAbsent(SKU_ID)).thenReturn(1);
        when(operationMapper.insertIgnore(any(InventoryOperation.class))).thenReturn(1);
        when(stockMapper.update(any(), any())).thenReturn(1);
        when(operationService.updateById(any(InventoryOperation.class))).thenReturn(true);
        when(stockMapper.selectById(SKU_ID)).thenReturn(stock(20, 0, 0));

        InventoryStockVO result = stockService.increaseAvailable(SKU_ID, 20, "INBOUND-1001", "首次入库");

        assertEquals(20, result.getAvailableStock());
        verify(skuService).requireSku(SKU_ID);
        verify(stockMapper).insertIfAbsent(SKU_ID);
    }

    @Test
    void releaseShouldReturnLockedQuantityToAvailable() {
        when(operationMapper.insertIgnore(any(InventoryOperation.class))).thenReturn(1);
        when(stockMapper.update(any(), any())).thenReturn(1);
        when(operationService.updateById(any(InventoryOperation.class))).thenReturn(true);
        when(stockMapper.selectById(SKU_ID)).thenReturn(stock(7, 3, 0), stock(10, 0, 0));

        InventoryStockVO result = stockService.releaseStock(SKU_ID, 3, "ORDER-1002");

        assertEquals(10, result.getAvailableStock());
        assertEquals(0, result.getLockedStock());
    }

    @Test
    void lockShouldRejectInsufficientAvailableStock() {
        when(stockMapper.selectById(SKU_ID)).thenReturn(stock(2, 0, 0));
        when(operationMapper.insertIgnore(any(InventoryOperation.class))).thenReturn(1);
        when(stockMapper.update(any(), any())).thenReturn(0);

        assertThrows(BusinessException.class, () -> stockService.lockStock(SKU_ID, 3, "ORDER-1003"));
    }

    private InventoryStock stock(int available, int locked, int sold) {
        InventoryStock stock = new InventoryStock();
        stock.setSkuId(SKU_ID);
        stock.setAvailableStock(available);
        stock.setLockedStock(locked);
        stock.setSoldStock(sold);
        return stock;
    }
}
