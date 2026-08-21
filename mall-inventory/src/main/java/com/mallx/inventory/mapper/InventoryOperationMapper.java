package com.mallx.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallx.inventory.entity.InventoryOperation;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

/** 库存流水数据访问接口。 */
@Mapper
public interface InventoryOperationMapper extends BaseMapper<InventoryOperation> {
    /** 通过唯一索引抢占库存操作，返回0表示相同业务操作已经执行或正在执行。 */
    @Insert("INSERT IGNORE INTO inventory_operation(id, business_no, sku_id, operation_type, quantity, status, remark) " +
            "VALUES(#{id}, #{businessNo}, #{skuId}, #{operationType}, #{quantity}, 0, #{remark})")
    int insertIgnore(InventoryOperation operation);
}
