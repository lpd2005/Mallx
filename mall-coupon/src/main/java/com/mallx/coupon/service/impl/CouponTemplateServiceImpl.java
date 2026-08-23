package com.mallx.coupon.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mallx.common.enums.ResultCode;
import com.mallx.common.exception.BusinessException;
import com.mallx.coupon.dto.CouponTemplateRequest;
import com.mallx.coupon.entity.CouponTemplate;
import com.mallx.coupon.enums.CouponTemplateStatus;
import com.mallx.coupon.enums.CouponType;
import com.mallx.coupon.mapper.CouponTemplateMapper;
import com.mallx.coupon.service.CouponTemplateService;
import com.mallx.coupon.vo.CouponTemplateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/** 优惠券模板配置与发放服务实现。 */
@Service
@RequiredArgsConstructor
public class CouponTemplateServiceImpl extends ServiceImpl<CouponTemplateMapper, CouponTemplate>
        implements CouponTemplateService {

    private static final int MAX_BATCH_SIZE = 1000;

    /** 自定义原子库存SQL通过显式注入的Mapper调用，不使用baseMapper。 */
    private final CouponTemplateMapper couponTemplateMapper;

    /** 新模板只能以草稿状态创建。 */
    @Override
    @Transactional
    public CouponTemplateVO createTemplate(CouponTemplateRequest request) {
        CouponTemplate template = new CouponTemplate();
        BeanUtil.copyProperties(request, template);
        normalizeAndValidate(template);
        template.setReceivedStock(0);
        template.setStatus(CouponTemplateStatus.DRAFT.getValue());
        if (!save(template)) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "优惠券模板创建失败");
        }
        return toVO(template);
    }

    /** 仅草稿允许修改，已经发布的规则保持不可变。 */
    @Override
    @Transactional
    public CouponTemplateVO updateTemplate(Long templateId, CouponTemplateRequest request) {
        CouponTemplate current = requireTemplate(templateId);
        if (!CouponTemplateStatus.DRAFT.matches(current.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "只有草稿优惠券可以修改");
        }
        BeanUtil.copyProperties(request, current);
        normalizeAndValidate(current);
        current.setReceivedStock(0);
        if (!updateById(current)) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "优惠券模板修改失败");
        }
        return toVO(current);
    }

    /** 发布后进入发放状态；实际能否领取仍由领取时间窗口判断。 */
    @Override
    @Transactional
    public CouponTemplateVO publishTemplate(Long templateId) {
        CouponTemplate template = requireTemplate(templateId);
        if (CouponTemplateStatus.ACTIVE.matches(template.getStatus())) {
            return toVO(template);
        }
        if (!CouponTemplateStatus.DRAFT.matches(template.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "当前优惠券模板不能发布");
        }
        normalizeAndValidate(template);
        LocalDateTime now = LocalDateTime.now();
        if (!template.getClaimEndAt().isAfter(now)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "领取结束时间必须晚于当前时间");
        }
        boolean updated = lambdaUpdate()
                .eq(CouponTemplate::getId, templateId)
                .eq(CouponTemplate::getStatus, CouponTemplateStatus.DRAFT.getValue())
                .set(CouponTemplate::getStatus, CouponTemplateStatus.ACTIVE.getValue())
                .update();
        if (!updated) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "优惠券模板状态已变化，请刷新后重试");
        }
        return toVO(requireTemplate(templateId));
    }

    /** 停止发放只阻止继续领取，不影响用户已经领取的优惠券。 */
    @Override
    @Transactional
    public CouponTemplateVO stopTemplate(Long templateId) {
        CouponTemplate template = requireTemplate(templateId);
        if (CouponTemplateStatus.STOPPED.matches(template.getStatus())) {
            return toVO(template);
        }
        if (!CouponTemplateStatus.ACTIVE.matches(template.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "只有发放中的优惠券可以停止");
        }
        boolean updated = lambdaUpdate()
                .eq(CouponTemplate::getId, templateId)
                .eq(CouponTemplate::getStatus, CouponTemplateStatus.ACTIVE.getValue())
                .set(CouponTemplate::getStatus, CouponTemplateStatus.STOPPED.getValue())
                .update();
        if (!updated) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "优惠券模板状态已变化，请刷新后重试");
        }
        return toVO(requireTemplate(templateId));
    }

    @Override
    public CouponTemplateVO getTemplate(Long templateId) {
        return toVO(requireTemplate(templateId));
    }

    /** 管理端按状态查询模板；状态为空时查询全部。 */
    @Override
    public List<CouponTemplateVO> listTemplates(Integer status) {
        if (status != null && (status < CouponTemplateStatus.DRAFT.getValue()
                || status > CouponTemplateStatus.ENDED.getValue())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "优惠券模板状态不正确");
        }
        return lambdaQuery()
                .eq(status != null, CouponTemplate::getStatus, status)
                .orderByDesc(CouponTemplate::getCreatedAt)
                .list()
                .stream()
                .map(this::toVO)
                .toList();
    }

    /** 优惠券中心只展示当前可领取且仍有剩余发行量的模板。 */
    @Override
    public List<CouponTemplateVO> listClaimableTemplates() {
        LocalDateTime now = LocalDateTime.now();
        return lambdaQuery()
                .eq(CouponTemplate::getStatus, CouponTemplateStatus.ACTIVE.getValue())
                .le(CouponTemplate::getClaimStartAt, now)
                .gt(CouponTemplate::getClaimEndAt, now)
                .apply("received_stock < total_stock")
                .orderByAsc(CouponTemplate::getClaimEndAt)
                .list()
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public CouponTemplate requireTemplate(Long templateId) {
        if (templateId == null || templateId <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "优惠券模板ID不正确");
        }
        CouponTemplate template = getById(templateId);
        if (template == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "优惠券模板不存在");
        }
        return template;
    }

    /** 自定义Mapper以单条条件SQL原子占用发行量。 */
    @Override
    public void occupyStock(Long templateId, LocalDateTime now) {
        if (couponTemplateMapper.increaseReceivedStock(templateId, now) == 1) {
            return;
        }
        CouponTemplate template = requireTemplate(templateId);
        if (!CouponTemplateStatus.ACTIVE.matches(template.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "优惠券当前不可领取");
        }
        if (now.isBefore(template.getClaimStartAt())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "优惠券领取尚未开始");
        }
        if (!now.isBefore(template.getClaimEndAt())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "优惠券领取已经结束");
        }
        throw new BusinessException(ResultCode.BAD_REQUEST, "优惠券已领完");
    }

    /** 定时任务分批收敛模板状态，领取正确性不依赖任务是否准时执行。 */
    @Override
    @Transactional
    public int endExpiredTemplates(int batchSize) {
        validateBatchSize(batchSize);
        LocalDateTime now = LocalDateTime.now();
        List<Long> templateIds = lambdaQuery()
                .select(CouponTemplate::getId)
                .eq(CouponTemplate::getStatus, CouponTemplateStatus.ACTIVE.getValue())
                .le(CouponTemplate::getClaimEndAt, now) // 领取截止时间在当前时间之前
                .orderByAsc(CouponTemplate::getClaimEndAt)
                .last("LIMIT " + batchSize)
                .list()
                .stream()
                .map(CouponTemplate::getId)
                .toList();
        if (templateIds.isEmpty()) {
            return 0;
        }
        int endedCount = 0;
        for (Long templateId : templateIds) {
            boolean updated = lambdaUpdate()
                    .eq(CouponTemplate::getId, templateId)
                    .eq(CouponTemplate::getStatus, CouponTemplateStatus.ACTIVE.getValue())
                    .le(CouponTemplate::getClaimEndAt, now)
                    .set(CouponTemplate::getStatus, CouponTemplateStatus.ENDED.getValue())
                    .update();
            if (updated) {
                endedCount++;
            }
        }
        return endedCount;
    }

    /** 统一校验不同券类型的互斥字段和时间范围。 */
    private void normalizeAndValidate(CouponTemplate template) {
        if (template.getCouponName() == null || template.getCouponName().isBlank()
                || template.getCouponName().trim().length() > 100) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "优惠券名称不能为空且不能超过100个字符");
        }
        template.setCouponName(template.getCouponName().trim());
        template.setThresholdAmount(money(template.getThresholdAmount()));
        CouponType type = CouponType.fromValue(template.getCouponType());
        if (type == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "优惠券类型不正确");
        }
        if (template.getThresholdAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "优惠券使用门槛不能小于0");
        }
        if (template.getTotalStock() == null || template.getTotalStock() <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "优惠券发行数量必须大于0");
        }
        if (type == CouponType.FULL_REDUCTION) {
            BigDecimal discountAmount = money(template.getDiscountAmount());
            if (discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "满减金额必须大于0");
            }
            template.setDiscountAmount(discountAmount);
            template.setDiscountRate(null);
            template.setMaxDiscountAmount(null);
        } else {
            BigDecimal rate = template.getDiscountRate();
            if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0 || rate.compareTo(BigDecimal.ONE) >= 0) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "折扣率必须大于0且小于1");
            }
            template.setDiscountRate(rate.setScale(4, RoundingMode.HALF_UP));
            template.setDiscountAmount(null);
            if (template.getMaxDiscountAmount() != null) {
                BigDecimal maximum = money(template.getMaxDiscountAmount());
                if (maximum.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessException(ResultCode.BAD_REQUEST, "最高优惠金额必须大于0");
                }
                template.setMaxDiscountAmount(maximum);
            }
        }
        if (template.getClaimStartAt() == null || template.getClaimEndAt() == null
                || !template.getClaimStartAt().isBefore(template.getClaimEndAt())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "优惠券领取时间范围不正确");
        }
        if (template.getValidStartAt() == null || template.getValidEndAt() == null
                || !template.getValidStartAt().isBefore(template.getValidEndAt())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "优惠券使用时间范围不正确");
        }
        if (!template.getValidEndAt().isAfter(template.getClaimEndAt())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "优惠券有效期结束时间必须晚于领取结束时间");
        }
    }

    private BigDecimal money(BigDecimal value) {
        if (value == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "优惠金额参数不能为空");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private void validateBatchSize(int batchSize) {
        if (batchSize <= 0 || batchSize > MAX_BATCH_SIZE) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "批次大小必须在1到1000之间");
        }
    }

    private CouponTemplateVO toVO(CouponTemplate template) {
        int received = template.getReceivedStock() == null ? 0 : template.getReceivedStock();
        int total = template.getTotalStock() == null ? 0 : template.getTotalStock();
        return CouponTemplateVO.builder()
                .id(template.getId())
                .couponName(template.getCouponName())
                .couponType(template.getCouponType())
                .thresholdAmount(template.getThresholdAmount())
                .discountAmount(template.getDiscountAmount())
                .discountRate(template.getDiscountRate())
                .maxDiscountAmount(template.getMaxDiscountAmount())
                .totalStock(template.getTotalStock())
                .receivedStock(template.getReceivedStock())
                .remainingStock(Math.max(total - received, 0))
                .claimStartAt(template.getClaimStartAt())
                .claimEndAt(template.getClaimEndAt())
                .validStartAt(template.getValidStartAt())
                .validEndAt(template.getValidEndAt())
                .status(template.getStatus())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }
}
