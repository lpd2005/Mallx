package com.mallx.user.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mallx.user.entity.UserMembershipLog;
import com.mallx.user.mapper.UserMembershipLogMapper;
import com.mallx.user.service.UserMembershipLogService;
import org.springframework.stereotype.Service;

/** 会员变更日志服务实现。 */
@Service
public class UserMembershipLogServiceImpl extends ServiceImpl<UserMembershipLogMapper, UserMembershipLog>
        implements UserMembershipLogService {
}
