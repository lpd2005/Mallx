package com.mallx.inventory.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mallx.common.enums.ResultCode;
import com.mallx.common.exception.BusinessException;
import com.mallx.inventory.entity.InventoryOperation;
import com.mallx.inventory.entity.InventoryStock;
import com.mallx.inventory.enums.InventoryOperationType;
import com.mallx.inventory.mapper.InventoryOperationMapper;
import com.mallx.inventory.mapper.InventoryStockMapper;
import com.mallx.inventory.service.InventoryOperationService;
import com.mallx.inventory.service.InventoryStockService;
import com.mallx.inventory.vo.InventoryStockVO;
import com.mallx.product.service.ProductSkuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 库存核心服务，所有数量变化均使用带下限条件的数据库原子更新。 */
@Service
@RequiredArgsConstructor
public class InventoryStockServiceImpl extends ServiceImpl<InventoryStockMapper, InventoryStock>
        implements InventoryStockService {
    private static final int OPERATION_SUCCESS = 1;

    private final InventoryStockMapper inventoryStockMapper;
    private final InventoryOperationMapper operationMapper;
    private final InventoryOperationService operationService;
    private final ProductSkuService skuService;

    /** 查询SKU当前库存，不存在时明确提示尚未初始化。 */
    @Override
    public InventoryStockVO getStock(Long skuId) {
        return toVO(requireStock(skuId));
    }

    /** 入库增加可用库存，并在首次入库时幂等创建库存记录。 */
    @Override
    @Transactional
    public InventoryStockVO increaseAvailable(Long skuId, int quantity, String businessNo, String remark) {
        validate(skuId, quantity, businessNo);
        skuService.requireSku(skuId);
        inventoryStockMapper.insertIfAbsent(skuId);
        InventoryOperation operation = beginOperation(
                skuId, quantity, businessNo, InventoryOperationType.INCREASE_AVAILABLE, remark);
        if (operation == null) return getStock(skuId);

        boolean updated = lambdaUpdate().eq(InventoryStock::getSkuId, skuId)
                .setSql("available_stock = available_stock + " + quantity).update();
        if (!updated) throw new BusinessException(ResultCode.NOT_FOUND, "SKU库存不存在");
        return finishOperation(operation, requireStock(skuId));
    }

    /** 原子地将可用库存转为锁定库存。 */
    @Override
    @Transactional
    public InventoryStockVO lockStock(Long skuId, int quantity, String businessNo) {
        validate(skuId, quantity, businessNo);
        requireStock(skuId);
        InventoryOperation operation = beginOperation(skuId, quantity, businessNo, InventoryOperationType.LOCK,
                "订单锁定库存");
        if (operation == null) return getStock(skuId);

        boolean updated = lambdaUpdate().eq(InventoryStock::getSkuId, skuId)
                .ge(InventoryStock::getAvailableStock, quantity)
                .setSql("available_stock = available_stock - " + quantity)
                .setSql("locked_stock = locked_stock + " + quantity).update();
        if (!updated) throw new BusinessException(ResultCode.BAD_REQUEST, "可用库存不足");
        return finishOperation(operation, requireStock(skuId));
    }

    /** 原子地将锁定库存转为已售库存。 */
    @Override
    @Transactional
    public InventoryStockVO deductStock(Long skuId, int quantity, String businessNo) {
        validate(skuId, quantity, businessNo);
        requireStock(skuId);
        InventoryOperation operation = beginOperation(skuId, quantity, businessNo, InventoryOperationType.DEDUCT,
                "订单确认扣减库存");
        if (operation == null) return getStock(skuId);

        boolean updated = lambdaUpdate().eq(InventoryStock::getSkuId, skuId)
                .ge(InventoryStock::getLockedStock, quantity)
                .setSql("locked_stock = locked_stock - " + quantity)
                .setSql("sold_stock = sold_stock + " + quantity).update();
        if (!updated) throw new BusinessException(ResultCode.BAD_REQUEST, "锁定库存不足，无法扣减");
        return finishOperation(operation, requireStock(skuId));
    }

    /** 原子地将锁定库存释放回可用库存。 */
    @Override
    @Transactional
    public InventoryStockVO releaseStock(Long skuId, int quantity, String businessNo) {
        validate(skuId, quantity, businessNo);
        requireStock(skuId);
        InventoryOperation operation = beginOperation(skuId, quantity, businessNo, InventoryOperationType.RELEASE,
                "订单取消释放库存");
        if (operation == null) return getStock(skuId);

        boolean updated = lambdaUpdate().eq(InventoryStock::getSkuId, skuId)
                .ge(InventoryStock::getLockedStock, quantity)
                .setSql("locked_stock = locked_stock - " + quantity)
                .setSql("available_stock = available_stock + " + quantity).update();
        if (!updated) throw new BusinessException(ResultCode.BAD_REQUEST, "锁定库存不足，无法释放");
        return finishOperation(operation, requireStock(skuId));
    }

    /** 先以唯一业务键插入处理中流水，插入失败表示相同操作已经完成。 */
    private InventoryOperation beginOperation(Long skuId, int quantity, String businessNo,
                                               InventoryOperationType type, String remark) {
        InventoryOperation operation = new InventoryOperation();
        operation.setId(IdWorker.getId());
        operation.setBusinessNo(businessNo.trim());
        operation.setSkuId(skuId);
        operation.setOperationType(type.getValue());
        operation.setQuantity(quantity);
        operation.setStatus(0);
        operation.setRemark(normalizeRemark(remark));
        if (operationMapper.insertIgnore(operation) == 1) return operation;

        InventoryOperation existing = operationService.lambdaQuery()
                .eq(InventoryOperation::getBusinessNo, businessNo.trim())
                .eq(InventoryOperation::getSkuId, skuId)
                .eq(InventoryOperation::getOperationType, type.getValue()).one();
        if (existing == null) throw new BusinessException(ResultCode.INTERNAL_ERROR, "库存幂等记录读取失败");
        if (!Integer.valueOf(quantity).equals(existing.getQuantity()))
            throw new BusinessException(ResultCode.BAD_REQUEST, "相同库存业务号的操作数量不一致");
        return null;
    }

    /** 根据操作后的库存反推操作前快照，并把幂等流水标记为成功。 */
    private InventoryStockVO finishOperation(InventoryOperation operation, InventoryStock after) {
        int quantity = operation.getQuantity();
        int beforeAvailable = after.getAvailableStock();
        int beforeLocked = after.getLockedStock();
        int beforeSold = after.getSoldStock();
        InventoryOperationType type = operationType(operation.getOperationType());
        switch (type) {
            case INCREASE_AVAILABLE -> beforeAvailable -= quantity;
            case LOCK -> { beforeAvailable += quantity; beforeLocked -= quantity; }
            case DEDUCT -> { beforeLocked += quantity; beforeSold -= quantity; }
            case RELEASE -> { beforeLocked += quantity; beforeAvailable -= quantity; }
        }
        operation.setBeforeAvailable(beforeAvailable);
        operation.setAfterAvailable(after.getAvailableStock());
        operation.setBeforeLocked(beforeLocked);
        operation.setAfterLocked(after.getLockedStock());
        operation.setBeforeSold(beforeSold);
        operation.setAfterSold(after.getSoldStock());
        operation.setStatus(OPERATION_SUCCESS);
        if (!operationService.updateById(operation))
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "库存流水保存失败");
        return toVO(after);
    }

    private InventoryOperationType operationType(Integer value) {
        for (InventoryOperationType type : InventoryOperationType.values())
            if (type.getValue() == value) return type;
        throw new BusinessException(ResultCode.INTERNAL_ERROR, "库存操作类型不正确");
    }

    private InventoryStock requireStock(Long skuId) {
        InventoryStock stock = getById(skuId);
        if (stock == null) throw new BusinessException(ResultCode.NOT_FOUND, "SKU库存尚未初始化");
        return stock;
    }

    private void validate(Long skuId, int quantity, String businessNo) {
        if (skuId == null || skuId <= 0) throw new BusinessException(ResultCode.BAD_REQUEST, "SKU ID不正确");
        if (quantity <= 0) throw new BusinessException(ResultCode.BAD_REQUEST, "库存数量必须大于0");
        if (businessNo == null || businessNo.isBlank() || businessNo.trim().length() > 64)
            throw new BusinessException(ResultCode.BAD_REQUEST, "库存业务号不能为空且不能超过64位");
    }

    private String normalizeRemark(String remark) {
        if (remark == null || remark.isBlank()) return null;
        String value = remark.trim();
        return value.length() > 255 ? value.substring(0, 255) : value;
    }

    private InventoryStockVO toVO(InventoryStock stock) {
        return InventoryStockVO.builder()
                .skuId(stock.getSkuId())
                .availableStock(stock.getAvailableStock())
                .lockedStock(stock.getLockedStock())
                .soldStock(stock.getSoldStock())
                .outOfStock(stock.getAvailableStock() == 0)
                .build();
    }
}
