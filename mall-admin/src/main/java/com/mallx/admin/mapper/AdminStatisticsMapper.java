package com.mallx.admin.mapper;

import com.mallx.admin.vo.*;

import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/** 跨表聚合统计放在XML中，只读访问，不在后台维护交易状态。 */
@Mapper
public interface AdminStatisticsMapper {

    AdminDashboardVO selectSummary(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    List<AdminTopProductVO> selectTopProducts(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("limit") int limit);
}
