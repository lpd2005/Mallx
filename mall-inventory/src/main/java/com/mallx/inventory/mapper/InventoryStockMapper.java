package com.mallx.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallx.inventory.entity.InventoryStock;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

/** SKU库存数据访问接口。 */
@Mapper
public interface InventoryStockMapper extends BaseMapper<InventoryStock> {
    /** SKU库存不存在时幂等初始化为0。 */
    @Insert("INSERT IGNORE INTO inventory_stock(sku_id, available_stock, locked_stock, sold_stock) " +
            "VALUES(#{skuId}, 0, 0, 0)")
    int insertIfAbsent(Long skuId);
}
