package com.mallx.coupon.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mallx.common.enums.ResultCode;
import com.mallx.common.exception.BusinessException;
import com.mallx.coupon.entity.CouponOperationLog;
import com.mallx.coupon.entity.CouponTemplate;
import com.mallx.coupon.entity.CouponUser;
import com.mallx.coupon.enums.CouponOperationType;
import com.mallx.coupon.enums.CouponTemplateStatus;
import com.mallx.coupon.enums.CouponType;
import com.mallx.coupon.enums.CouponUserStatus;
import com.mallx.coupon.mapper.CouponUserMapper;
import com.mallx.coupon.service.CouponOperationLogService;
import com.mallx.coupon.service.CouponTemplateService;
import com.mallx.coupon.service.CouponUserService;
import com.mallx.coupon.vo.CouponCalculationVO;
import com.mallx.coupon.vo.CouponUserVO;
import com.mallx.coupon.vo.UsableCouponVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 用户优惠券领取、计算和订单状态流转服务实现。 */
@Service
@RequiredArgsConstructor
public class CouponUserServiceImpl extends ServiceImpl<CouponUserMapper, CouponUser>
        implements CouponUserService {

    private static final int MAX_BATCH_SIZE = 1000;

    /** 行锁查询通过显式注入的Mapper调用。 */
    private final CouponUserMapper couponUserMapper;
    private final CouponTemplateService couponTemplateService;
    private final CouponOperationLogService operationLogService;

    /**
     * 领取事务先原子占用模板发行量，再插入用户券。
     * 如果唯一索引发现重复领取，整个事务回滚，模板发行量也会自动恢复。
     */
    @Override
    @Transactional
    public CouponUserVO claimCoupon(Long userId, Long templateId) {
        requireUserId(userId);
        LocalDateTime now = LocalDateTime.now();
        CouponTemplate template = couponTemplateService.requireTemplate(templateId);
        validateClaimable(template, now);
        if (lambdaQuery()
                .eq(CouponUser::getUserId, userId)
                .eq(CouponUser::getTemplateId, templateId)
                .count() > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "你已经领取过该优惠券");
        }

        couponTemplateService.occupyStock(templateId, now);
        CouponUser couponUser = new CouponUser();
        couponUser.setTemplateId(templateId);
        couponUser.setUserId(userId);
        couponUser.setStatus(CouponUserStatus.UNUSED.getValue());
        couponUser.setReceivedAt(now);
        couponUser.setValidStartAt(template.getValidStartAt());
        couponUser.setValidEndAt(template.getValidEndAt());
        try {
            if (!save(couponUser)) {
                throw new BusinessException(ResultCode.INTERNAL_ERROR, "优惠券领取失败");
            }
        } catch (DuplicateKeyException exception) {
            // 并发重复请求最终由数据库唯一索引裁决，抛出异常使本事务中的库存占用一并回滚。
            throw new BusinessException(ResultCode.BAD_REQUEST, "你已经领取过该优惠券");
        }
        saveLog(operationNo(CouponOperationType.CLAIM, couponUser.getId(), null), couponUser,
                CouponOperationType.CLAIM, null, CouponUserStatus.UNUSED.getValue(), null, "领取优惠券");
        return toVO(couponUser, template);
    }

    /** 查询前实时收敛当前用户的过期状态，避免依赖定时任务准点执行。 */
    @Override
    @Transactional
    public List<CouponUserVO> listUserCoupons(Long userId, Integer status) {
        requireUserId(userId);
        validateStatus(status);
        expireUserCoupons(userId, LocalDateTime.now());
        List<CouponUser> couponUsers = lambdaQuery()
                .eq(CouponUser::getUserId, userId)
                .eq(status != null, CouponUser::getStatus, status)
                .orderByDesc(CouponUser::getReceivedAt)
                .list();
        return toUserVOs(couponUsers);
    }

    /** 查询指定金额下未使用、在有效期内并达到门槛的优惠券。 */
    @Override
    @Transactional
    public List<UsableCouponVO> listUsableCoupons(Long userId, BigDecimal eligibleAmount) {
        requireUserId(userId);
        BigDecimal amount = requireAmount(eligibleAmount);
        LocalDateTime now = LocalDateTime.now();
        expireUserCoupons(userId, now);
        List<CouponUser> couponUsers = lambdaQuery()
                .eq(CouponUser::getUserId, userId)
                .eq(CouponUser::getStatus, CouponUserStatus.UNUSED.getValue())
                .le(CouponUser::getValidStartAt, now)
                .gt(CouponUser::getValidEndAt, now)
                .orderByAsc(CouponUser::getValidEndAt)
                .list();
        Map<Long, CouponTemplate> templates = templateMap(couponUsers);
        return couponUsers.stream()
                .filter(coupon -> templates.containsKey(coupon.getTemplateId()))
                .filter(coupon -> amount.compareTo(templates.get(coupon.getTemplateId()).getThresholdAmount()) >= 0)
                .map(coupon -> {
                    CouponTemplate template = templates.get(coupon.getTemplateId());
                    CouponCalculationVO calculation = calculateRule(coupon.getId(), template, amount);
                    return UsableCouponVO.builder()
                            .coupon(toVO(coupon, template))
                            .discountAmount(calculation.getDiscountAmount())
                            .payableAmount(calculation.getPayableAmount())
                            .build();
                })
                .toList();
    }

    /** 纯计算接口不会占用优惠券，订单提交时仍必须调用锁券接口。 */
    @Override
    public CouponCalculationVO calculateDiscount(Long userId, Long couponUserId, BigDecimal eligibleAmount) {
        requireUserId(userId);
        BigDecimal amount = requireAmount(eligibleAmount);
        CouponUser couponUser = requireCouponUser(couponUserId);
        requireOwner(couponUser, userId);
        validateUnusedAndEffective(couponUser, LocalDateTime.now());
        return calculateRule(couponUserId,
                couponTemplateService.requireTemplate(couponUser.getTemplateId()), amount);
    }

    /**
     * 订单提交时锁定用户券，同一订单重复调用保持幂等。
     * 行锁保证同一张券不能同时绑定到两个订单。
     */
    @Override
    @Transactional
    public CouponCalculationVO lockForOrder(Long userId, Long couponUserId, String orderNo,
                                             BigDecimal eligibleAmount) {
        requireUserId(userId);
        String normalizedOrderNo = requireOrderNo(orderNo);
        BigDecimal amount = requireAmount(eligibleAmount);
        CouponUser couponUser = lockCouponUser(couponUserId);
        requireOwner(couponUser, userId);
        CouponTemplate template = couponTemplateService.requireTemplate(couponUser.getTemplateId());

        if ((CouponUserStatus.LOCKED.matches(couponUser.getStatus())
                || CouponUserStatus.USED.matches(couponUser.getStatus()))
                && normalizedOrderNo.equals(couponUser.getOrderNo())) {
            return calculateRule(couponUserId, template, amount);
        }
        validateUnusedAndEffective(couponUser, LocalDateTime.now());
        CouponCalculationVO calculation = calculateRule(couponUserId, template, amount);
        Integer beforeStatus = couponUser.getStatus();
        couponUser.setStatus(CouponUserStatus.LOCKED.getValue());
        couponUser.setOrderNo(normalizedOrderNo);
        couponUser.setLockedAt(LocalDateTime.now());
        try {
            if (!updateById(couponUser)) {
                throw new BusinessException(ResultCode.INTERNAL_ERROR, "优惠券锁定失败");
            }
        } catch (DuplicateKeyException exception) {
            // order_no唯一索引保证一个订单最多绑定一张优惠券。
            throw new BusinessException(ResultCode.BAD_REQUEST, "当前订单已经使用其他优惠券");
        }
        saveLog(operationNo(CouponOperationType.LOCK, couponUserId, normalizedOrderNo), couponUser,
                CouponOperationType.LOCK, beforeStatus, CouponUserStatus.LOCKED.getValue(),
                normalizedOrderNo, "订单提交锁定优惠券");
        return calculation;
    }

    /** 订单创建成功后确认使用；相同订单重复确认保持幂等。 */
    @Override
    @Transactional
    public void confirmUsed(Long couponUserId, String orderNo) {
        String normalizedOrderNo = requireOrderNo(orderNo);
        CouponUser couponUser = lockCouponUser(couponUserId);
        if (CouponUserStatus.USED.matches(couponUser.getStatus())
                && normalizedOrderNo.equals(couponUser.getOrderNo())) {
            return;
        }
        if (!CouponUserStatus.LOCKED.matches(couponUser.getStatus())
                || !normalizedOrderNo.equals(couponUser.getOrderNo())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "优惠券未被当前订单锁定");
        }
        Integer beforeStatus = couponUser.getStatus();
        couponUser.setStatus(CouponUserStatus.USED.getValue());
        couponUser.setUsedAt(LocalDateTime.now());
        if (!updateById(couponUser)) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "优惠券使用状态更新失败");
        }
        saveLog(operationNo(CouponOperationType.USE, couponUserId, normalizedOrderNo), couponUser,
                CouponOperationType.USE, beforeStatus, CouponUserStatus.USED.getValue(),
                normalizedOrderNo, "订单创建成功使用优惠券");
    }

    /** 订单取消时回滚优惠券；已过有效期则直接转为过期。 */
    @Override
    @Transactional
    public void rollbackForOrder(Long couponUserId, String orderNo) {
        String normalizedOrderNo = requireOrderNo(orderNo);
        CouponUser couponUser = lockCouponUser(couponUserId);
        if (CouponUserStatus.UNUSED.matches(couponUser.getStatus())
                || CouponUserStatus.EXPIRED.matches(couponUser.getStatus())) {
            return;
        }
        if ((!CouponUserStatus.LOCKED.matches(couponUser.getStatus())
                && !CouponUserStatus.USED.matches(couponUser.getStatus()))
                || !normalizedOrderNo.equals(couponUser.getOrderNo())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "优惠券不属于当前订单");
        }
        Integer beforeStatus = couponUser.getStatus();
        int afterStatus = couponUser.getValidEndAt().isAfter(LocalDateTime.now())
                ? CouponUserStatus.UNUSED.getValue()
                : CouponUserStatus.EXPIRED.getValue();
        couponUser.setStatus(afterStatus);
        couponUser.setOrderNo(null);
        couponUser.setLockedAt(null);
        couponUser.setUsedAt(null);
        if (!updateById(couponUser)) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "优惠券回滚失败");
        }
        saveLog(operationNo(CouponOperationType.ROLLBACK, couponUserId, normalizedOrderNo), couponUser,
                CouponOperationType.ROLLBACK, beforeStatus, afterStatus,
                normalizedOrderNo, "订单取消回滚优惠券");
    }

    /** 分批把有效期已结束的未使用优惠券更新为已过期。 */
    @Override
    @Transactional
    public int expireUnusedCoupons(int batchSize) {
        validateBatchSize(batchSize);
        LocalDateTime now = LocalDateTime.now();
        List<CouponUser> expiredCoupons = lambdaQuery()
                .eq(CouponUser::getStatus, CouponUserStatus.UNUSED.getValue())
                .le(CouponUser::getValidEndAt, now)
                .orderByAsc(CouponUser::getValidEndAt)
                .last("LIMIT " + batchSize)
                .list();
        int count = 0;
        for (CouponUser couponUser : expiredCoupons) {
            if (expireCoupon(couponUser, now)) {
                count++;
            }
        }
        return count;
    }

    /** 只收敛指定用户的过期券，通常数量很少。 */
    private void expireUserCoupons(Long userId, LocalDateTime now) {
        List<CouponUser> expiredCoupons = lambdaQuery()
                .eq(CouponUser::getUserId, userId)
                .eq(CouponUser::getStatus, CouponUserStatus.UNUSED.getValue())
                .le(CouponUser::getValidEndAt, now)
                .list();
        for (CouponUser couponUser : expiredCoupons) {
            expireCoupon(couponUser, now);
        }
    }

    /** 条件更新保证定时任务和实时查询并发执行时只记录一次过期流水。 */
    private boolean expireCoupon(CouponUser couponUser, LocalDateTime now) {
        boolean updated = lambdaUpdate()
                .eq(CouponUser::getId, couponUser.getId())
                .eq(CouponUser::getStatus, CouponUserStatus.UNUSED.getValue())
                .le(CouponUser::getValidEndAt, now)
                .set(CouponUser::getStatus, CouponUserStatus.EXPIRED.getValue())
                .update();
        if (!updated) {
            return false;
        }
        saveLog(operationNo(CouponOperationType.EXPIRE, couponUser.getId(), null), couponUser,
                CouponOperationType.EXPIRE, CouponUserStatus.UNUSED.getValue(),
                CouponUserStatus.EXPIRED.getValue(), null, "优惠券到期自动失效");
        return true;
    }

    /** 根据券类型计算优惠，金额统一保留两位小数。 */
    private CouponCalculationVO calculateRule(Long couponUserId, CouponTemplate template, BigDecimal amount) {
        if (amount.compareTo(template.getThresholdAmount()) < 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "当前金额未达到优惠券使用门槛");
        }
        CouponType type = CouponType.fromValue(template.getCouponType());
        if (type == null) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "优惠券类型配置不正确");
        }
        BigDecimal discount;
        if (type == CouponType.FULL_REDUCTION) {
            discount = template.getDiscountAmount();
        } else {
            discount = amount.multiply(BigDecimal.ONE.subtract(template.getDiscountRate()))
                    .setScale(2, RoundingMode.HALF_UP);
            if (template.getMaxDiscountAmount() != null
                    && discount.compareTo(template.getMaxDiscountAmount()) > 0) {
                discount = template.getMaxDiscountAmount();
            }
        }
        // 优惠金额最多等于可优惠金额，避免产生负数应付金额。
        discount = discount.min(amount).setScale(2, RoundingMode.HALF_UP);
        return CouponCalculationVO.builder()
                .couponUserId(couponUserId)
                .eligibleAmount(amount)
                .discountAmount(discount)
                .payableAmount(amount.subtract(discount).setScale(2, RoundingMode.HALF_UP))
                .build();
    }

    private void validateClaimable(CouponTemplate template, LocalDateTime now) {
        if (!CouponTemplateStatus.ACTIVE.matches(template.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "优惠券当前不可领取");
        }
        if (now.isBefore(template.getClaimStartAt())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "优惠券领取尚未开始");
        }
        if (!now.isBefore(template.getClaimEndAt())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "优惠券领取已经结束");
        }
        int receivedStock = template.getReceivedStock() == null ? 0 : template.getReceivedStock();
        if (receivedStock >= template.getTotalStock()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "优惠券已领完");
        }
    }

    private void validateUnusedAndEffective(CouponUser couponUser, LocalDateTime now) {
        if (!CouponUserStatus.UNUSED.matches(couponUser.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "优惠券当前不可使用");
        }
        if (now.isBefore(couponUser.getValidStartAt())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "优惠券尚未到使用时间");
        }
        if (!now.isBefore(couponUser.getValidEndAt())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "优惠券已过期");
        }
    }

    private CouponUser lockCouponUser(Long couponUserId) {
        requireCouponUserId(couponUserId);
        CouponUser couponUser = couponUserMapper.selectByIdForUpdate(couponUserId);
        if (couponUser == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户优惠券不存在");
        }
        return couponUser;
    }

    private CouponUser requireCouponUser(Long couponUserId) {
        requireCouponUserId(couponUserId);
        CouponUser couponUser = getById(couponUserId);
        if (couponUser == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户优惠券不存在");
        }
        return couponUser;
    }

    private List<CouponUserVO> toUserVOs(List<CouponUser> couponUsers) {
        Map<Long, CouponTemplate> templates = templateMap(couponUsers);
        return couponUsers.stream()
                .filter(coupon -> templates.containsKey(coupon.getTemplateId()))
                .map(coupon -> toVO(coupon, templates.get(coupon.getTemplateId())))
                .toList();
    }

    /** 批量读取模板，避免用户券列表产生N+1查询。 */
    private Map<Long, CouponTemplate> templateMap(List<CouponUser> couponUsers) {
        if (couponUsers.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> templateIds = couponUsers.stream()
                .map(CouponUser::getTemplateId)
                .distinct()
                .toList();
        return couponTemplateService.lambdaQuery()
                .in(CouponTemplate::getId, templateIds)
                .list()
                .stream()
                .collect(Collectors.toMap(CouponTemplate::getId, Function.identity()));
    }

    private CouponUserVO toVO(CouponUser couponUser, CouponTemplate template) {
        return CouponUserVO.builder()
                .id(couponUser.getId())
                .templateId(couponUser.getTemplateId())
                .couponName(template.getCouponName())
                .couponType(template.getCouponType())
                .thresholdAmount(template.getThresholdAmount())
                .discountAmount(template.getDiscountAmount())
                .discountRate(template.getDiscountRate())
                .maxDiscountAmount(template.getMaxDiscountAmount())
                .status(couponUser.getStatus())
                .orderNo(couponUser.getOrderNo())
                .receivedAt(couponUser.getReceivedAt())
                .validStartAt(couponUser.getValidStartAt())
                .validEndAt(couponUser.getValidEndAt())
                .lockedAt(couponUser.getLockedAt())
                .usedAt(couponUser.getUsedAt())
                .build();
    }

    private void saveLog(String operationNo, CouponUser couponUser, CouponOperationType type,
                         Integer beforeStatus, Integer afterStatus, String orderNo, String remark) {
        CouponOperationLog log = new CouponOperationLog();
        log.setOperationNo(operationNo);
        log.setUserCouponId(couponUser.getId());
        log.setUserId(couponUser.getUserId());
        log.setOrderNo(orderNo);
        log.setOperationType(type.getValue());
        log.setBeforeStatus(beforeStatus);
        log.setAfterStatus(afterStatus);
        log.setRemark(remark);
        if (!operationLogService.save(log)) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "优惠券操作流水保存失败");
        }
    }

    private String operationNo(CouponOperationType type, Long couponUserId, String orderNo) {
        return type.name() + ":" + couponUserId + (orderNo == null ? "" : ":" + orderNo);
    }

    private void requireOwner(CouponUser couponUser, Long userId) {
        if (!userId.equals(couponUser.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作该优惠券");
        }
    }

    private BigDecimal requireAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "可优惠金额必须大于0");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private String requireOrderNo(String orderNo) {
        if (orderNo == null || orderNo.isBlank() || orderNo.trim().length() > 64) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单号不能为空且不能超过64位");
        }
        return orderNo.trim();
    }

    private void requireUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "当前用户身份无效");
        }
    }

    private void requireCouponUserId(Long couponUserId) {
        if (couponUserId == null || couponUserId <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户优惠券ID不正确");
        }
    }

    private void validateStatus(Integer status) {
        if (status != null && !CouponUserStatus.supports(status)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户优惠券状态不正确");
        }
    }

    private void validateBatchSize(int batchSize) {
        if (batchSize <= 0 || batchSize > MAX_BATCH_SIZE) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "批次大小必须在1到1000之间");
        }
    }
}
