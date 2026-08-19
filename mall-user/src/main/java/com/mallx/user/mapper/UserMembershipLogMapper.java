package com.mallx.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallx.user.entity.UserMembershipLog;
import org.apache.ibatis.annotations.Mapper;

/** 会员变更日志数据访问层。 */
@Mapper
public interface UserMembershipLogMapper extends BaseMapper<UserMembershipLog> {
}
