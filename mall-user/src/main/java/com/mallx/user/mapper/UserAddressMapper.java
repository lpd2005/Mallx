package com.mallx.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallx.user.entity.UserAddress;
import org.apache.ibatis.annotations.Mapper;

/** 用户收货地址数据访问接口。 */
@Mapper
public interface UserAddressMapper extends BaseMapper<UserAddress> {
}
