package com.mallx.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallx.payment.entity.PaymentRecord;

import org.apache.ibatis.annotations.*;

/** 支付单Mapper。 */
@Mapper
public interface PaymentRecordMapper extends BaseMapper<PaymentRecord> {
    @Select("SELECT * FROM payment_record WHERE payment_no=#{paymentNo} FOR UPDATE")
    PaymentRecord selectByPaymentNoForUpdate(@Param("paymentNo") String paymentNo);
}
