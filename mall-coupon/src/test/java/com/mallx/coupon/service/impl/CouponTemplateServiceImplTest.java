package com.mallx.coupon.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.mallx.common.exception.BusinessException;
import com.mallx.coupon.dto.CouponTemplateRequest;
import com.mallx.coupon.entity.CouponTemplate;
import com.mallx.coupon.enums.CouponTemplateStatus;
import com.mallx.coupon.enums.CouponType;
import com.mallx.coupon.mapper.CouponTemplateMapper;
import com.mallx.coupon.vo.CouponTemplateVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 优惠券模板规则和发布后不可修改测试。 */
@ExtendWith(MockitoExtension.class)
class CouponTemplateServiceImplTest {

    @BeforeAll
    static void initializeMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(configuration, "coupon-template-test"), CouponTemplate.class);
    }

    @Mock
    private CouponTemplateMapper couponTemplateMapper;
    private CouponTemplateServiceImpl couponTemplateService;

    @BeforeEach
    void setUp() {
        couponTemplateService = new CouponTemplateServiceImpl(couponTemplateMapper);
        ReflectionTestUtils.setField(couponTemplateService, "baseMapper", couponTemplateMapper);
    }

    /** 满减券创建时应清理只属于折扣券的字段。 */
    @Test
    void createFullReductionTemplateShouldNormalizeRuleFields() {
        CouponTemplateRequest request = validRequest();
        request.setCouponType(CouponType.FULL_REDUCTION.getValue());
        request.setDiscountAmount(new BigDecimal("20"));
        request.setDiscountRate(new BigDecimal("0.8000"));
        request.setMaxDiscountAmount(new BigDecimal("50"));
        when(couponTemplateMapper.insert(any(CouponTemplate.class))).thenReturn(1);

        CouponTemplateVO result = couponTemplateService.createTemplate(request);

        ArgumentCaptor<CouponTemplate> captor = ArgumentCaptor.forClass(CouponTemplate.class);
        verify(couponTemplateMapper).insert(captor.capture());
        CouponTemplate saved = captor.getValue();
        assertEquals(new BigDecimal("20.00"), saved.getDiscountAmount());
        assertNull(saved.getDiscountRate());
        assertNull(saved.getMaxDiscountAmount());
        assertEquals(CouponTemplateStatus.DRAFT.getValue(), result.getStatus());
        assertEquals(100, result.getRemainingStock());
    }

    /** 折扣率等于1没有实际优惠，应拒绝创建。 */
    @Test
    void createDiscountTemplateShouldRejectInvalidRate() {
        CouponTemplateRequest request = validRequest();
        request.setCouponType(CouponType.DISCOUNT.getValue());
        request.setDiscountRate(BigDecimal.ONE);

        assertThrows(BusinessException.class, () -> couponTemplateService.createTemplate(request));
    }

    /** 已经发布的模板不能再修改优惠规则。 */
    @Test
    void activeTemplateShouldBeImmutable() {
        CouponTemplate active = new CouponTemplate();
        active.setId(1001L);
        active.setStatus(CouponTemplateStatus.ACTIVE.getValue());
        when(couponTemplateMapper.selectById(1001L)).thenReturn(active);

        assertThrows(BusinessException.class,
                () -> couponTemplateService.updateTemplate(1001L, validRequest()));
    }

    private CouponTemplateRequest validRequest() {
        LocalDateTime now = LocalDateTime.now();
        CouponTemplateRequest request = new CouponTemplateRequest();
        request.setCouponName("满100减20");
        request.setCouponType(CouponType.FULL_REDUCTION.getValue());
        request.setThresholdAmount(new BigDecimal("100"));
        request.setDiscountAmount(new BigDecimal("20"));
        request.setTotalStock(100);
        request.setClaimStartAt(now.plusHours(1));
        request.setClaimEndAt(now.plusDays(2));
        request.setValidStartAt(now.plusHours(1));
        request.setValidEndAt(now.plusDays(10));
        return request;
    }
}
