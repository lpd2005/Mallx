package com.mallx.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mallx.admin.entity.AdminOperationLog;
import com.mallx.admin.mapper.AdminOperationLogMapper;
import com.mallx.admin.service.AdminOperationLogService;
import com.mallx.common.context.CurrentUserProvider;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.Supplier;

/** 只记录成功变更；鉴权失败/业务失败不伪造成功流水。 READ_COMMITTED保证RBAC等待行锁后读取最新关联，避免继承旧的一致性快照。 */
@Service
@RequiredArgsConstructor
public class AdminOperationLogServiceImpl
        extends ServiceImpl<AdminOperationLogMapper, AdminOperationLog>
        implements AdminOperationLogService {
    private final CurrentUserProvider currentUserProvider;

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public <T> T execute(String action, String target, Supplier<T> operation) {
        Long operatorId = currentUserProvider.requireUserId();
        T result = operation.get();
        AdminOperationLog log = new AdminOperationLog();
        log.setOperatorId(operatorId);
        log.setAction(action);
        // 创建操作没有现成ID，从领域服务返回的实体取生成后的主键。
        Object createdId =
                (target == null && result != null) ? BeanUtil.getProperty(result, "id") : null;
        log.setTarget(target != null ? target : Objects.toString(createdId, "new"));
        log.setCreatedAt(LocalDateTime.now());
        if (!save(log)) throw new IllegalStateException("后台审计流水保存失败");
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void execute(String action, String target, Runnable operation) {
        execute(
                action,
                target,
                () -> {
                    operation.run();
                    return null;
                });
    }
}
