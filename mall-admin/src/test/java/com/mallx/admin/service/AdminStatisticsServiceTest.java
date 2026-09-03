package com.mallx.admin.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.mallx.admin.mapper.AdminStatisticsMapper;
import com.mallx.admin.service.impl.AdminStatisticsServiceImpl;
import com.mallx.admin.vo.AdminDashboardVO;
import com.mallx.common.exception.BusinessException;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.*;

/** 验证日期边界和退款统计口径。 */
class AdminStatisticsServiceTest {
    @Test
    void usesHalfOpenDatesAndAllowsNegativeNetReceipt() {
        AdminStatisticsMapper mapper = mock(AdminStatisticsMapper.class);
        LocalDate start = LocalDate.of(2026, 8, 1);
        LocalDate end = LocalDate.of(2026, 8, 31);
        when(mapper.selectSummary(start.atStartOfDay(), end.plusDays(1).atStartOfDay()))
                .thenReturn(
                        AdminDashboardVO.builder()
                                .paidAmount(new BigDecimal("100"))
                                .refundAmount(new BigDecimal("120"))
                                .build());
        AdminDashboardVO summary = new AdminStatisticsServiceImpl(mapper).summary(start, end);
        assertEquals(new BigDecimal("-20"), summary.getNetReceiptAmount());
        assertEquals(start, summary.getStartDate());
        assertEquals(end, summary.getEndDate());
    }

    @Test
    void rejectsReversedOrTooWideRangesBeforeQuery() {
        AdminStatisticsMapper mapper = mock(AdminStatisticsMapper.class);
        AdminStatisticsServiceImpl service = new AdminStatisticsServiceImpl(mapper);
        LocalDate start = LocalDate.of(2026, 1, 1);
        assertThrows(BusinessException.class, () -> service.summary(start, start.minusDays(1)));
        assertThrows(BusinessException.class, () -> service.summary(start, start.plusDays(93)));
        verifyNoInteractions(mapper);
    }

    @Test
    void capsTopProductsLimit() {
        AdminStatisticsMapper mapper = mock(AdminStatisticsMapper.class);
        AdminStatisticsServiceImpl service = new AdminStatisticsServiceImpl(mapper);
        assertThrows(BusinessException.class, () -> service.topProducts(null, null, 21));
        assertThrows(BusinessException.class, () -> service.topProducts(null, null, 0));
        verifyNoInteractions(mapper);
    }
}
