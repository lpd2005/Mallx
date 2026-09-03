package com.mallx.admin.service.impl;

import com.mallx.admin.mapper.AdminStatisticsMapper;
import com.mallx.admin.service.AdminStatisticsService;
import com.mallx.admin.vo.*;
import com.mallx.common.enums.ResultCode;
import com.mallx.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** 按北京时间确定自然日；跨零点、跨日退款按各自发生时间计入。 */
@Service
@RequiredArgsConstructor
public class AdminStatisticsServiceImpl implements AdminStatisticsService {
    private final AdminStatisticsMapper statisticsMapper;

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardVO summary(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        LocalDate start = startDate == null ? today : startDate;
        LocalDate end = endDate == null ? today : endDate;
        validateRange(start, end);
        AdminDashboardVO summary =
                statisticsMapper.selectSummary(
                        start.atStartOfDay(), end.plusDays(1).atStartOfDay());
        summary.setStartDate(start);
        summary.setEndDate(end);
        summary.setNetReceiptAmount(summary.getPaidAmount().subtract(summary.getRefundAmount()));
        return summary;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminTopProductVO> topProducts(LocalDate startDate, LocalDate endDate, int limit) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        LocalDate start = startDate == null ? today.minusDays(6) : startDate;
        LocalDate end = endDate == null ? today : endDate;
        validateRange(start, end);
        if (limit < 1 || limit > 20)
            throw new BusinessException(ResultCode.BAD_REQUEST, "热销榜最多20条");
        return statisticsMapper.selectTopProducts(
                start.atStartOfDay(), end.plusDays(1).atStartOfDay(), limit);
    }

    private void validateRange(LocalDate start, LocalDate end) {
        if (start.isAfter(end)
                || ChronoUnit.DAYS.between(start, end) >= 93
                || end.equals(LocalDate.MAX))
            throw new BusinessException(ResultCode.BAD_REQUEST, "统计日期范围应为1至93天");
    }
}
