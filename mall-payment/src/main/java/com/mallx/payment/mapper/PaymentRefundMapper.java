package com.mallx.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallx.payment.entity.PaymentRefund;

import org.apache.ibatis.annotations.Mapper;

/** 退款成功流水Mapper。 */
@Mapper
public interface PaymentRefundMapper extends BaseMapper<PaymentRefund> {}
