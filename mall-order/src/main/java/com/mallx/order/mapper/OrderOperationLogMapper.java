package com.mallx.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallx.order.entity.OrderOperationLog;

import org.apache.ibatis.annotations.Mapper;

/** 订单状态流水Mapper。 */
@Mapper
public interface OrderOperationLogMapper extends BaseMapper<OrderOperationLog> {}
