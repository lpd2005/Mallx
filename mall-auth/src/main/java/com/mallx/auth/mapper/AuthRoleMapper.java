package com.mallx.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 认证角色查询接口。
 */
@Mapper
public interface AuthRoleMapper {

    /**
     * 查询账号拥有的有效角色编码。
     *
     * @param accountId 账号主键
     * @return 角色编码列表
     */
    List<String> selectRoleCodesByAccountId(@Param("accountId") Long accountId);


}
