package com.mallx.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallx.admin.entity.AdminOperationLog;

import org.apache.ibatis.annotations.Mapper;

/** 审计流水使用MyBatis-Plus标准持久化。 */
@Mapper
public interface AdminOperationLogMapper extends BaseMapper<AdminOperationLog> {}
