package com.mallx.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallx.order.entity.MallOrder;

import org.apache.ibatis.annotations.*;

/** 订单主表Mapper。 */
@Mapper
public interface MallOrderMapper extends BaseMapper<MallOrder> {
    @Select("SELECT * FROM mall_order WHERE order_no=#{orderNo} FOR UPDATE")
    MallOrder selectByOrderNoForUpdate(@Param("orderNo") String orderNo);
}
