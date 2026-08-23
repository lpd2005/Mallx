package com.mallx.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallx.coupon.entity.CouponUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 用户优惠券数据访问接口。 */
@Mapper
public interface CouponUserMapper extends BaseMapper<CouponUser> {

    /** 锁定用户券记录，串行处理同一张券的锁定、使用和回滚。 */
    @Select("SELECT * FROM coupon_user WHERE id = #{id} FOR UPDATE")
    CouponUser selectByIdForUpdate(@Param("id") Long id);
}
