package com.mallx.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallx.payment.entity.PaymentRecord;

import org.apache.ibatis.annotations.*;

/** 支付单Mapper。 */
@Mapper
public interface PaymentRecordMapper extends BaseMapper<PaymentRecord> {
    @Select("SELECT * FROM payment_record WHERE payment_no=#{paymentNo} FOR UPDATE")
    PaymentRecord selectByPaymentNoForUpdate(@Param("paymentNo") String paymentNo);

    /** 整单退款使用订单唯一索引锁定原支付记录。 */
    @Select("SELECT * FROM payment_record WHERE order_no=#{orderNo} FOR UPDATE")
    PaymentRecord selectByOrderNoForUpdate(@Param("orderNo") String orderNo);
}
