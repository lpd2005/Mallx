package com.mallx.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallx.coupon.entity.CouponTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/** 优惠券模板数据访问接口。 */
@Mapper
public interface CouponTemplateMapper extends BaseMapper<CouponTemplate> {

    /**
     * 在发放时间内原子增加已领取数量，条件不满足时返回0。
     * 该SQL避免“先查库存再更新”导致并发超发。
     */
    @Update("UPDATE coupon_template SET received_stock = received_stock + 1 " +
            "WHERE id = #{templateId} AND status = 1 " +
            "AND claim_start_at <= #{now} AND claim_end_at > #{now} " +
            "AND received_stock < total_stock")
    int increaseReceivedStock(@Param("templateId") Long templateId, @Param("now") LocalDateTime now);
}
