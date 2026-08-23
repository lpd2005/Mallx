package com.mallx.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallx.coupon.entity.CouponOperationLog;
import org.apache.ibatis.annotations.Mapper;

/** 优惠券操作流水数据访问接口。 */
@Mapper
public interface CouponOperationLogMapper extends BaseMapper<CouponOperationLog> {
}
