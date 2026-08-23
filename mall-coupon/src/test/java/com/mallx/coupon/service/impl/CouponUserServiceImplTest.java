package com.mallx.coupon.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.mallx.common.exception.BusinessException;
import com.mallx.coupon.entity.CouponOperationLog;
import com.mallx.coupon.entity.CouponTemplate;
import com.mallx.coupon.entity.CouponUser;
import com.mallx.coupon.enums.CouponTemplateStatus;
import com.mallx.coupon.enums.CouponType;
import com.mallx.coupon.enums.CouponUserStatus;
import com.mallx.coupon.mapper.CouponUserMapper;
import com.mallx.coupon.service.CouponOperationLogService;
import com.mallx.coupon.service.CouponTemplateService;
import com.mallx.coupon.vo.CouponCalculationVO;
import com.mallx.coupon.vo.CouponUserVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 优惠券领取、金额计算和订单状态流转测试。 */
@ExtendWith(MockitoExtension.class)
class CouponUserServiceImplTest {

    private static final Long USER_ID = 1001L;
    private static final Long TEMPLATE_ID = 2001L;
    private static final Long COUPON_USER_ID = 3001L;

    @BeforeAll
    static void initializeMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(configuration, "coupon-user-test"), CouponUser.class);
    }

    @Mock
    private CouponUserMapper couponUserMapper;
    @Mock
    private CouponTemplateService couponTemplateService;
    @Mock
    private CouponOperationLogService operationLogService;
    private CouponUserServiceImpl couponUserService;

    @BeforeEach
    void setUp() {
        couponUserService = new CouponUserServiceImpl(
                couponUserMapper, couponTemplateService, operationLogService) {
            @Override
            public Class<CouponUser> getEntityClass() {
                return CouponUser.class;
            }

            @Override
            public Class<CouponUserMapper> getMapperClass() {
                return CouponUserMapper.class;
            }
        };
        ReflectionTestUtils.setField(couponUserService, "baseMapper", couponUserMapper);
    }

    /** 领取成功时应原子占用一份模板库存并保存用户券和领取流水。 */
    @Test
    void claimShouldOccupyStockAndCreateUserCoupon() {
        CouponTemplate template = fullReductionTemplate();
        when(couponTemplateService.requireTemplate(TEMPLATE_ID)).thenReturn(template);
        when(couponUserMapper.selectCount(any())).thenReturn(0L);
        when(couponUserMapper.insert(any(CouponUser.class))).thenAnswer(invocation -> {
            CouponUser couponUser = invocation.getArgument(0);
            couponUser.setId(COUPON_USER_ID);
            return 1;
        });
        when(operationLogService.save(any(CouponOperationLog.class))).thenReturn(true);

        CouponUserVO result = couponUserService.claimCoupon(USER_ID, TEMPLATE_ID);

        assertEquals(COUPON_USER_ID, result.getId());
        assertEquals(CouponUserStatus.UNUSED.getValue(), result.getStatus());
        verify(couponTemplateService).occupyStock(any(), any());
        verify(operationLogService).save(any(CouponOperationLog.class));
    }

    /** 计算折扣券时必须应用最高优惠金额。 */
    @Test
    void discountCouponShouldApplyMaximumDiscount() {
        CouponUser couponUser = unusedCoupon();
        CouponTemplate template = discountTemplate();
        when(couponUserMapper.selectById(COUPON_USER_ID)).thenReturn(couponUser);
        when(couponTemplateService.requireTemplate(TEMPLATE_ID)).thenReturn(template);

        CouponCalculationVO result = couponUserService.calculateDiscount(
                USER_ID, COUPON_USER_ID, new BigDecimal("500"));

        assertEquals(new BigDecimal("50.00"), result.getDiscountAmount());
        assertEquals(new BigDecimal("450.00"), result.getPayableAmount());
    }

    /** 未达到满减门槛时不得使用优惠券。 */
    @Test
    void fullReductionCouponShouldRejectAmountBelowThreshold() {
        when(couponUserMapper.selectById(COUPON_USER_ID)).thenReturn(unusedCoupon());
        when(couponTemplateService.requireTemplate(TEMPLATE_ID)).thenReturn(fullReductionTemplate());

        assertThrows(BusinessException.class, () -> couponUserService.calculateDiscount(
                USER_ID, COUPON_USER_ID, new BigDecimal("99.99")));
    }

    /** 用户券应按锁定、使用、订单取消回滚的顺序流转。 */
    @Test
    void couponShouldFlowFromLockedToUsedThenRollback() {
        CouponUser couponUser = unusedCoupon();
        when(couponUserMapper.selectByIdForUpdate(COUPON_USER_ID)).thenReturn(couponUser);
        when(couponTemplateService.requireTemplate(TEMPLATE_ID)).thenReturn(fullReductionTemplate());
        when(couponUserMapper.updateById(any(CouponUser.class))).thenReturn(1);
        when(operationLogService.save(any(CouponOperationLog.class))).thenReturn(true);

        couponUserService.lockForOrder(USER_ID, COUPON_USER_ID, "ORDER-1001", new BigDecimal("100"));
        assertEquals(CouponUserStatus.LOCKED.getValue(), couponUser.getStatus());

        couponUserService.confirmUsed(COUPON_USER_ID, "ORDER-1001");
        assertEquals(CouponUserStatus.USED.getValue(), couponUser.getStatus());

        couponUserService.rollbackForOrder(COUPON_USER_ID, "ORDER-1001");
        assertEquals(CouponUserStatus.UNUSED.getValue(), couponUser.getStatus());
        assertNull(couponUser.getOrderNo());
        verify(operationLogService, times(3)).save(any(CouponOperationLog.class));
    }

    /** 订单取消时优惠券已经超过有效期，应回滚为已过期而不是未使用。 */
    @Test
    void rollbackAfterValidityShouldExpireCoupon() {
        CouponUser couponUser = unusedCoupon();
        couponUser.setStatus(CouponUserStatus.USED.getValue());
        couponUser.setOrderNo("ORDER-1002");
        couponUser.setValidEndAt(LocalDateTime.now().minusSeconds(1));
        when(couponUserMapper.selectByIdForUpdate(COUPON_USER_ID)).thenReturn(couponUser);
        when(couponUserMapper.updateById(any(CouponUser.class))).thenReturn(1);
        when(operationLogService.save(any(CouponOperationLog.class))).thenReturn(true);

        couponUserService.rollbackForOrder(COUPON_USER_ID, "ORDER-1002");

        assertEquals(CouponUserStatus.EXPIRED.getValue(), couponUser.getStatus());
    }

    private CouponUser unusedCoupon() {
        CouponUser couponUser = new CouponUser();
        couponUser.setId(COUPON_USER_ID);
        couponUser.setTemplateId(TEMPLATE_ID);
        couponUser.setUserId(USER_ID);
        couponUser.setStatus(CouponUserStatus.UNUSED.getValue());
        couponUser.setReceivedAt(LocalDateTime.now().minusDays(1));
        couponUser.setValidStartAt(LocalDateTime.now().minusDays(1));
        couponUser.setValidEndAt(LocalDateTime.now().plusDays(10));
        return couponUser;
    }

    private CouponTemplate fullReductionTemplate() {
        CouponTemplate template = baseTemplate();
        template.setCouponName("满100减20");
        template.setCouponType(CouponType.FULL_REDUCTION.getValue());
        template.setThresholdAmount(new BigDecimal("100.00"));
        template.setDiscountAmount(new BigDecimal("20.00"));
        return template;
    }

    private CouponTemplate discountTemplate() {
        CouponTemplate template = baseTemplate();
        template.setCouponName("全场8折券");
        template.setCouponType(CouponType.DISCOUNT.getValue());
        template.setThresholdAmount(new BigDecimal("100.00"));
        template.setDiscountRate(new BigDecimal("0.8000"));
        template.setMaxDiscountAmount(new BigDecimal("50.00"));
        return template;
    }

    private CouponTemplate baseTemplate() {
        CouponTemplate template = new CouponTemplate();
        template.setId(TEMPLATE_ID);
        template.setStatus(CouponTemplateStatus.ACTIVE.getValue());
        template.setTotalStock(100);
        template.setReceivedStock(0);
        template.setClaimStartAt(LocalDateTime.now().minusDays(1));
        template.setClaimEndAt(LocalDateTime.now().plusDays(2));
        template.setValidStartAt(LocalDateTime.now().minusDays(1));
        template.setValidEndAt(LocalDateTime.now().plusDays(10));
        return template;
    }
}
