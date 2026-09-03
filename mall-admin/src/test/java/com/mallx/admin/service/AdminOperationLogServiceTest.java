package com.mallx.admin.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.mallx.admin.entity.AdminOperationLog;
import com.mallx.admin.mapper.AdminOperationLogMapper;
import com.mallx.admin.service.impl.AdminOperationLogServiceImpl;
import com.mallx.common.context.CurrentUserProvider;
import com.mallx.product.entity.ProductSpu;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.sql.Connection;
import java.util.function.Supplier;

import javax.sql.DataSource;

/** 业务结果、真实操作人、审计主键及事务回滚边界测试；不替代真实MySQL联调。 */
class AdminOperationLogServiceTest {
    @Test
    void createdObjectIdAndActualOperatorAreRecorded() {
        CurrentUserProvider users = mock(CurrentUserProvider.class);
        when(users.requireUserId()).thenReturn(7L);
        AdminOperationLogMapper mapper = mock(AdminOperationLogMapper.class);
        when(mapper.insert(any(AdminOperationLog.class))).thenReturn(1);
        AdminOperationLogServiceImpl service = new AdminOperationLogServiceImpl(users);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ProductSpu spu = new ProductSpu();
        spu.setId(88L);
        assertSame(spu, service.execute("product:create-spu", null, () -> spu));
        ArgumentCaptor<AdminOperationLog> logs = ArgumentCaptor.forClass(AdminOperationLog.class);
        verify(mapper).insert(logs.capture());
        assertEquals(7L, logs.getValue().getOperatorId());
        assertEquals("88", logs.getValue().getTarget());
        assertNotNull(logs.getValue().getCreatedAt());
    }

    @Test
    void failedBusinessOperationDoesNotRecordSuccess() {
        CurrentUserProvider users = mock(CurrentUserProvider.class);
        when(users.requireUserId()).thenReturn(7L);
        AdminOperationLogMapper mapper = mock(AdminOperationLogMapper.class);
        AdminOperationLogServiceImpl service = new AdminOperationLogServiceImpl(users);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        assertThrows(
                IllegalStateException.class,
                () ->
                        service.execute(
                                "test",
                                "1",
                                (Runnable)
                                        () -> {
                                            throw new IllegalStateException("failed");
                                        }));
        verifyNoInteractions(mapper);
    }

    @Test
    void auditInsertFailureRollsBackOuterTransaction() throws Exception {
        CurrentUserProvider users = mock(CurrentUserProvider.class);
        when(users.requireUserId()).thenReturn(7L);
        AdminOperationLogMapper mapper = mock(AdminOperationLogMapper.class);
        when(mapper.insert(any(AdminOperationLog.class))).thenReturn(0);
        AdminOperationLogServiceImpl target = new AdminOperationLogServiceImpl(users);
        ReflectionTestUtils.setField(target, "baseMapper", mapper);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        when(connection.getTransactionIsolation())
                .thenReturn(Connection.TRANSACTION_READ_COMMITTED);
        TransactionInterceptor transaction = new TransactionInterceptor();
        transaction.setTransactionManager(new DataSourceTransactionManager(dataSource));
        transaction.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        ProxyFactory factory = new ProxyFactory(target);
        factory.addAdvice(transaction);
        AdminOperationLogService proxy = (AdminOperationLogService) factory.getProxy();
        assertThrows(
                IllegalStateException.class,
                () -> proxy.execute("test", "1", (Supplier<String>) () -> "changed"));
        verify(connection).rollback();
        verify(connection, never()).commit();
    }
}
