package com.mallx.admin.service;

import com.mallx.admin.vo.*;

import java.time.LocalDate;
import java.util.List;

/** 后台统计查询，最多查询93个自然日，不计算没有埋点基础的转化率。 */
public interface AdminStatisticsService {
    AdminDashboardVO summary(LocalDate startDate, LocalDate endDate);

    List<AdminTopProductVO> topProducts(LocalDate startDate, LocalDate endDate, int limit);
}
