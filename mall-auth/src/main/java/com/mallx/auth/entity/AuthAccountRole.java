package com.mallx.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 账号与角色的多对多关联。
 */
@Data
@TableName("auth_account_role")
public class AuthAccountRole {

    private Long accountId;

    private Long roleId;
}
