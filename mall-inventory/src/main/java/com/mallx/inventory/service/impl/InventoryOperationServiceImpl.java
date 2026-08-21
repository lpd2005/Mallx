package com.mallx.inventory.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mallx.inventory.entity.InventoryOperation;
import com.mallx.inventory.mapper.InventoryOperationMapper;
import com.mallx.inventory.service.InventoryOperationService;
import com.mallx.inventory.vo.InventoryOperationVO;
import org.springframework.stereotype.Service;

import java.util.List;

/** 库存流水服务实现。 */
@Service
public class InventoryOperationServiceImpl extends ServiceImpl<InventoryOperationMapper, InventoryOperation>
        implements InventoryOperationService {
    /** 查询业务单号对应的全部成功流水。 */
    @Override
    public List<InventoryOperationVO> listByBusinessNo(String businessNo) {
        return lambdaQuery().eq(InventoryOperation::getBusinessNo, businessNo)
                .eq(InventoryOperation::getStatus, 1).orderByAsc(InventoryOperation::getCreatedAt)
                .list().stream().map(this::toVO).toList();
    }

    private InventoryOperationVO toVO(InventoryOperation operation) {
        return InventoryOperationVO.builder().businessNo(operation.getBusinessNo()).skuId(operation.getSkuId())
                .operationType(operation.getOperationType()).quantity(operation.getQuantity())
                .beforeAvailable(operation.getBeforeAvailable()).afterAvailable(operation.getAfterAvailable())
                .beforeLocked(operation.getBeforeLocked()).afterLocked(operation.getAfterLocked())
                .beforeSold(operation.getBeforeSold()).afterSold(operation.getAfterSold())
                .createdAt(operation.getCreatedAt()).build();
    }
}
