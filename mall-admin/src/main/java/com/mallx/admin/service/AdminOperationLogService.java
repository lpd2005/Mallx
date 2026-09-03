package com.mallx.admin.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.mallx.admin.entity.AdminOperationLog;

import java.util.function.Supplier;

/** 统一后台变更事务入口，写审计失败时业务修改一并回滚。 */
public interface AdminOperationLogService extends IService<AdminOperationLog> {
    <T> T execute(String action, String target, Supplier<T> operation);

    void execute(String action, String target, Runnable operation);
}
