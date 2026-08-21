package com.mallx.inventory.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.mallx.inventory.entity.InventoryOperation;
import com.mallx.inventory.vo.InventoryOperationVO;

import java.util.List;

/** 库存流水查询与持久化服务。 */
public interface InventoryOperationService extends IService<InventoryOperation> {
    /** 根据业务单号查询所有成功库存操作。 */
    List<InventoryOperationVO> listByBusinessNo(String businessNo);
}
