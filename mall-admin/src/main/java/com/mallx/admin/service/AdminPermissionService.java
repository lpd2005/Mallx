package com.mallx.admin.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.mallx.auth.entity.AuthPermission;

/** 复用auth权限表，管理端只公开查询及角色授权，不允许动态创建通配权限。 */
public interface AdminPermissionService extends IService<AuthPermission> {}
