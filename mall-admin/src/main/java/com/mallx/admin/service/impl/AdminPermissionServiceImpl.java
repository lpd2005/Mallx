package com.mallx.admin.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mallx.admin.service.AdminPermissionService;
import com.mallx.auth.entity.AuthPermission;
import com.mallx.auth.mapper.AuthPermissionMapper;

import org.springframework.stereotype.Service;

/** 权限目录由版本化SQL维护。 */
@Service
public class AdminPermissionServiceImpl extends ServiceImpl<AuthPermissionMapper, AuthPermission>
        implements AdminPermissionService {}
