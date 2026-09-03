package com.mallx.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallx.auth.entity.AuthPermission;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 认证权限查询接口。 */
@Mapper
public interface AuthPermissionMapper extends BaseMapper<AuthPermission> {

    /**
     * 查询账号通过有效角色拥有的权限编码。
     *
     * @param accountId 账号主键
     * @return 权限编码列表
     */
    List<String> selectPermissionCodesByAccountId(@Param("accountId") Long accountId);
}
