package com.mallx.inventory.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.mallx.inventory.entity.InventoryStock;
import com.mallx.inventory.vo.InventoryStockVO;

import java.util.Collection;
import java.util.List;

/** SKU库存业务服务。 */
public interface InventoryStockService extends IService<InventoryStock> {
    /** 查询SKU当前库存。 */
    InventoryStockVO getStock(Long skuId);
    /** 批量查询SKU当前库存，不存在的库存记录不会出现在结果中。 */
    List<InventoryStockVO> listStocks(Collection<Long> skuIds);
    /** 通过入库等业务增加可用库存。 */
    InventoryStockVO increaseAvailable(Long skuId, int quantity, String businessNo, String remark);
    /** 下单时将可用库存转为锁定库存。 */
    InventoryStockVO lockStock(Long skuId, int quantity, String businessNo);
    /** 支付或履约确认后将锁定库存转为已售库存。 */
    InventoryStockVO deductStock(Long skuId, int quantity, String businessNo);
    /** 订单取消时将锁定库存释放回可用库存。 */
    InventoryStockVO releaseStock(Long skuId, int quantity, String businessNo);

    /** 整单售后回补可销售库存；售后单号作为幂等业务号，历史累计已售数量保持不变。 */
    InventoryStockVO restockAfterSale(Long skuId, int quantity, String afterSaleNo);
}
