package com.mallx.coupon.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mallx.coupon.entity.CouponOperationLog;
import com.mallx.coupon.mapper.CouponOperationLogMapper;
import com.mallx.coupon.service.CouponOperationLogService;
import org.springframework.stereotype.Service;

/** 优惠券操作流水服务实现。 */
@Service
public class CouponOperationLogServiceImpl
        extends ServiceImpl<CouponOperationLogMapper, CouponOperationLog>
        implements CouponOperationLogService {
}
