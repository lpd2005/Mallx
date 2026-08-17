package com.mallx.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallx.auth.entity.AuthAccount;
import org.apache.ibatis.annotations.Mapper;

/**
 * 认证账号数据访问接口。
 */
@Mapper
public interface AuthAccountMapper extends BaseMapper<AuthAccount> {
}
