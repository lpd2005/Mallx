package com.mallx.aftersale.service.impl;

import cn.hutool.core.bean.BeanUtil;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mallx.aftersale.dto.AuditAfterSaleRequest;
import com.mallx.aftersale.dto.CreateAfterSaleRequest;
import com.mallx.aftersale.dto.ReceiveReturnRequest;
import com.mallx.aftersale.dto.ReturnShipmentRequest;
import com.mallx.aftersale.entity.AfterSale;
import com.mallx.aftersale.entity.AfterSaleLog;
import com.mallx.aftersale.enums.AfterSaleOperationType;
import com.mallx.aftersale.enums.AfterSaleStatus;
import com.mallx.aftersale.enums.AfterSaleType;
import com.mallx.aftersale.mapper.AfterSaleMapper;
import com.mallx.aftersale.service.AfterSaleLogService;
import com.mallx.aftersale.service.AfterSaleService;
import com.mallx.aftersale.vo.AfterSaleLogVO;
import com.mallx.aftersale.vo.AfterSaleVO;
import com.mallx.common.enums.ResultCode;
import com.mallx.common.exception.BusinessException;
import com.mallx.common.result.PageResult;
import com.mallx.order.service.OrderService;
import com.mallx.order.vo.OrderAfterSaleVO;
import com.mallx.payment.service.PaymentRefundService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

/** 售后、订单、模拟退款、库存和优惠券共用一个本地事务，任何一步失败均回滚。 */
@Service
@RequiredArgsConstructor
public class AfterSaleServiceImpl extends ServiceImpl<AfterSaleMapper, AfterSale>
        implements AfterSaleService {
    private final AfterSaleMapper afterSaleMapper;
    private final AfterSaleLogService logService;
    private final OrderService orderService;
    private final PaymentRefundService refundService;

    /**
     * 申请售后
     */
    @Override
    @Transactional
    public AfterSaleVO apply(Long userId, CreateAfterSaleRequest request) {
        requireUser(userId);
        if (request == null) {
            throw bad("售后申请不能为空");
        }
        AfterSaleType type = AfterSaleType.fromValue(request.getType());
        if (type == null) {
            throw bad("售后类型只支持1仅退款、2退货退款");
        }
        String orderNo = requiredText(request.getOrderNo(), 64, "订单号");
        String reason = requiredText(request.getReason(), 500, "售后原因");
        String afterSaleNo = "AS" + IdWorker.getIdStr();
        // 先锁订单，再判断是否已发货或已有售后，防止申请与发货并发穿透。
        OrderAfterSaleVO order =
                orderService.beginAfterSale(userId, orderNo, afterSaleNo, type == AfterSaleType.RETURN_REFUND);
        LocalDateTime now = LocalDateTime.now();
        AfterSale sale = new AfterSale();
        sale.setAfterSaleNo(afterSaleNo);
        sale.setOrderId(order.getOrderId());
        sale.setOrderNo(order.getOrderNo());
        sale.setActiveOrderNo(order.getOrderNo());
        sale.setUserId(userId);
        sale.setType(type.getValue());
        sale.setStatus(AfterSaleStatus.WAIT_AUDIT.getValue());
        sale.setOriginalOrderStatus(order.getOriginalStatus());
        sale.setRefundAmount(order.getPayAmount());
        sale.setReason(reason);
        // 未发货商品仍在库内，可直接回补；退货商品必须经过验收才能确定是否回补。
        sale.setRestock(type == AfterSaleType.REFUND_ONLY ? 1 : null);
        sale.setCreatedAt(now);
        sale.setUpdatedAt(now);
        if (!save(sale)) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "售后申请保存失败");
        }
        saveLog(sale, null, AfterSaleOperationType.APPLY, userId, reason);
        return toVO(sale);
    }

    /**
     *  我的售后
     */
    @Override
    public PageResult<AfterSaleVO> pageMine(Long userId, Integer status, int current, int size) {
        requireUser(userId);
        return queryPage(userId, status, current, size);
    }

    /**
     * 管理员售后
     */
    @Override
    public PageResult<AfterSaleVO> pageManagement(Integer status, int current, int size) {
        return queryPage(null, status, current, size);
    }

    /**
     * 查询我的售后
     */
    @Override
    public AfterSaleVO getMine(Long userId, String afterSaleNo) {
        requireUser(userId);
        AfterSale sale =
                lambdaQuery()
                        .eq(AfterSale::getAfterSaleNo, requireNo(afterSaleNo))
                        .eq(AfterSale::getUserId, userId)
                        .one();
        return detail(requireFound(sale));
    }

    @Override
    public AfterSaleVO getManagement(String afterSaleNo) {
        return detail(
                requireFound(
                        lambdaQuery().eq(AfterSale::getAfterSaleNo, requireNo(afterSaleNo)).one()));
    }

    /** 用户只可在寄回商品之前撤销，避免退款和在途退货被任意取消。 */
    @Override
    @Transactional
    public AfterSaleVO cancel(Long userId, String afterSaleNo) {
        AfterSale sale = lockOwned(userId, afterSaleNo);
        if (AfterSaleStatus.CANCELED.matches(sale.getStatus())) {
            return toVO(sale);
        }
        boolean canCancel =
                AfterSaleStatus.WAIT_AUDIT.matches(sale.getStatus())
                        || AfterSaleStatus.WAIT_RETURN.matches(sale.getStatus())
                        || (sale.getType() == AfterSaleType.REFUND_ONLY.getValue()
                                && AfterSaleStatus.APPROVED.matches(sale.getStatus()));
        if (!canCancel) {
            throw bad("当前售后阶段不能撤销");
        }
        orderService.restoreAfterSale(userId, sale.getOrderNo(), sale.getAfterSaleNo(), userId);
        Integer before = sale.getStatus();
        sale.setStatus(AfterSaleStatus.CANCELED.getValue());
        sale.setActiveOrderNo(null);
        sale.setCanceledAt(LocalDateTime.now());
        updateRequired(sale);
        saveLog(sale, before, AfterSaleOperationType.CANCEL, userId, "用户撤销售后");
        return toVO(sale);
    }

    /** 审核通过不直接退款：退货类型须寄回并验收，仅退款也须显式执行退款动作。 */
    @Override
    @Transactional
    public AfterSaleVO audit(Long operatorId, String afterSaleNo, AuditAfterSaleRequest request) {
        requireUser(operatorId);
        if (request == null || request.getApproved() == null) {
            throw bad("审核结果不能为空");
        }
        String remark = requiredText(request.getRemark(), 255, "审核说明");
        AfterSale sale = lock(afterSaleNo);
        requireStatus(sale, AfterSaleStatus.WAIT_AUDIT);
        Integer before = sale.getStatus();
        boolean approved = Boolean.TRUE.equals(request.getApproved());
        if (approved) {
            sale.setStatus(
                    sale.getType() == AfterSaleType.REFUND_ONLY.getValue()
                            ? AfterSaleStatus.APPROVED.getValue()
                            : AfterSaleStatus.WAIT_RETURN.getValue());
        } else {
            orderService.restoreAfterSale(
                    sale.getUserId(), sale.getOrderNo(), sale.getAfterSaleNo(), operatorId);
            sale.setStatus(AfterSaleStatus.REJECTED.getValue());
            sale.setActiveOrderNo(null);
        }
        sale.setAuditorId(operatorId);
        sale.setAuditRemark(remark);
        sale.setAuditedAt(LocalDateTime.now());
        updateRequired(sale);
        saveLog(sale, before, approved ? AfterSaleOperationType.APPROVE : AfterSaleOperationType.REJECT, operatorId, remark);
        return toVO(sale);
    }

    /**
     * 提交退货物流
     */
    @Override
    @Transactional
    public AfterSaleVO submitReturn(Long userId, String afterSaleNo, ReturnShipmentRequest request) {
        if (request == null) {
            throw bad("退货物流不能为空");
        }
        String carrier = requiredText(request.getCarrier(), 64, "物流公司");
        String trackingNo = requiredText(request.getTrackingNo(), 64, "物流单号");
        AfterSale sale = lockOwned(userId, afterSaleNo);
        requireReturnType(sale);
        if (sale.getReturnShippedAt() != null) {
            if (!carrier.equals(sale.getReturnCarrier())
                    || !trackingNo.equals(sale.getReturnTrackingNo())) {
                throw bad("已提交的退货物流不能被重复请求覆盖");
            }
            return toVO(sale);
        }
        requireStatus(sale, AfterSaleStatus.WAIT_RETURN);
        Integer before = sale.getStatus();
        sale.setReturnCarrier(carrier);
        sale.setReturnTrackingNo(trackingNo);
        sale.setReturnShippedAt(LocalDateTime.now());
        sale.setStatus(AfterSaleStatus.WAIT_RECEIVE.getValue());
        updateRequired(sale);
        saveLog(sale, before, AfterSaleOperationType.RETURN_SHIP, userId, carrier + "：" + trackingNo);
        return toVO(sale);
    }

    /**
     * 验收退货
     */
    @Override
    @Transactional
    public AfterSaleVO receiveReturn(
            Long operatorId, String afterSaleNo, ReceiveReturnRequest request) {
        requireUser(operatorId);
        if (request == null || request.getRestock() == null) {
            throw bad("必须明确退货是否可重新销售");
        }
        String remark = requiredText(request.getRemark(), 255, "验收说明");
        int restock = Boolean.TRUE.equals(request.getRestock()) ? 1 : 0;
        AfterSale sale = lock(afterSaleNo);
        requireReturnType(sale);
        if (sale.getReceivedAt() != null) {
            if (!Objects.equals(restock, sale.getRestock())) {
                throw bad("不能通过重复验收修改库存回补结论");
            }
            return toVO(sale);
        }
        requireStatus(sale, AfterSaleStatus.WAIT_RECEIVE);
        Integer before = sale.getStatus();
        sale.setRestock(restock);
        sale.setReceivedAt(LocalDateTime.now());
        sale.setStatus(AfterSaleStatus.APPROVED.getValue());
        updateRequired(sale);
        saveLog(sale, before, AfterSaleOperationType.RECEIVE, operatorId, remark);
        return toVO(sale);
    }

    /** 锁顺序为售后单→支付单→订单→库存/优惠券，退款完成后的重试直接返回原结果。 */
    @Override
    @Transactional
    public AfterSaleVO refund(Long operatorId, String afterSaleNo) {
        requireUser(operatorId);
        AfterSale sale = lock(afterSaleNo);
        if (AfterSaleStatus.COMPLETED.matches(sale.getStatus())) {
            return toVO(sale);
        }
        requireStatus(sale, AfterSaleStatus.APPROVED);
        if (sale.getRestock() == null
                || (sale.getType() == AfterSaleType.RETURN_REFUND.getValue()
                        && sale.getReceivedAt() == null)) {
            throw bad("退货尚未验收，不能退款");
        }
        String refundNo =
                refundService.refundForAfterSale(sale.getUserId(), sale.getOrderNo(), sale.getAfterSaleNo(), sale.getRefundAmount());
        // 后续库存、订单或流水失败会把模拟退款一并回滚，不吞掉异常。
        orderService.completeAfterSale(sale.getUserId(), sale.getOrderNo(), sale.getAfterSaleNo(), Integer.valueOf(1).equals(sale.getRestock()));
        Integer before = sale.getStatus();
        sale.setRefundNo(refundNo);
        sale.setStatus(AfterSaleStatus.COMPLETED.getValue());
        sale.setCompletedAt(LocalDateTime.now());
        updateRequired(sale);
        saveLog(sale, before, AfterSaleOperationType.REFUND, operatorId, "整单模拟退款成功：" + refundNo);
        return toVO(sale);
    }

    private PageResult<AfterSaleVO> queryPage(Long userId, Integer status, int current, int size) {
        if (current < 1
                || size < 1
                || size > 100
                || (status != null && !AfterSaleStatus.supports(status))) {
            throw bad("分页参数或售后状态不正确，每页最多100条");
        }
        Page<AfterSale> page =
                lambdaQuery()
                        .eq(userId != null, AfterSale::getUserId, userId)
                        .eq(status != null, AfterSale::getStatus, status)
                        .orderByDesc(AfterSale::getCreatedAt)
                        .orderByDesc(AfterSale::getId)
                        .page(new Page<>(current, size));
        return new PageResult<>(
                BeanUtil.copyToList(page.getRecords(), AfterSaleVO.class),
                page.getTotal(),
                page.getCurrent(),
                page.getSize());
    }

    private AfterSaleVO detail(AfterSale sale) {
        AfterSaleVO result = toVO(sale);
        result.setLogs(
                BeanUtil.copyToList(
                        logService
                                .lambdaQuery()
                                .eq(AfterSaleLog::getAfterSaleNo, sale.getAfterSaleNo())
                                .orderByAsc(AfterSaleLog::getId)
                                .list(),
                        AfterSaleLogVO.class));
        return result;
    }

    private AfterSale lockOwned(Long userId, String afterSaleNo) {
        requireUser(userId);
        AfterSale sale = lock(afterSaleNo);
        if (!userId.equals(sale.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "售后单不存在");
        }
        return sale;
    }

    private AfterSale lock(String afterSaleNo) {
        return requireFound(afterSaleMapper.selectByAfterSaleNoForUpdate(requireNo(afterSaleNo)));
    }

    private AfterSale requireFound(AfterSale sale) {
        if (sale == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "售后单不存在");
        }
        return sale;
    }

    private void requireStatus(AfterSale sale, AfterSaleStatus expected) {
        if (!expected.matches(sale.getStatus())) {
            throw bad("当前售后状态不允许该操作");
        }
    }

    private void requireReturnType(AfterSale sale) {
        if (sale.getType() != AfterSaleType.RETURN_REFUND.getValue()) {
            throw bad("仅退货退款需要处理退货物流");
        }
    }

    private void updateRequired(AfterSale sale) {
        sale.setUpdatedAt(LocalDateTime.now());
        if (!updateById(sale)) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "售后状态更新失败");
        }
    }

    private void saveLog(
            AfterSale sale,
            Integer before,
            AfterSaleOperationType operation,
            Long operatorId,
            String remark) {
        AfterSaleLog log = new AfterSaleLog();
        log.setAfterSaleNo(sale.getAfterSaleNo());
        log.setBeforeStatus(before);
        log.setAfterStatus(sale.getStatus());
        log.setOperationType(operation.getValue());
        log.setOperatorId(operatorId);
        log.setRemark(remark);
        log.setCreatedAt(LocalDateTime.now());
        if (!logService.save(log)) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "售后流水保存失败");
        }
    }

    private AfterSaleVO toVO(AfterSale sale) {
        return BeanUtil.copyProperties(sale, AfterSaleVO.class);
    }

    private String requireNo(String afterSaleNo) {
        return requiredText(afterSaleNo, 64, "售后单号");
    }

    private String requiredText(String value, int maxLength, String fieldName) {
        if (value == null || value.isBlank() || value.trim().length() > maxLength) {
            throw bad(fieldName + "不能为空且不能超过" + maxLength + "个字符");
        }
        return value.trim();
    }

    private void requireUser(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "当前用户身份无效");
        }
    }

    private BusinessException bad(String message) {
        return new BusinessException(ResultCode.BAD_REQUEST, message);
    }
}
