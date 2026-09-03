package com.mallx.admin.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.mallx.admin.dto.AdminPageQuery;
import com.mallx.admin.vo.AdminAccountVO;
import com.mallx.auth.entity.AuthAccount;
import com.mallx.common.result.PageResult;

import java.util.List;

/** 只维护账号状态和角色，不提供密码摘要或任意账号实体写入接口。 */
public interface AdminAccountService extends IService<AuthAccount> {
    PageResult<AdminAccountVO> pageAccounts(AdminPageQuery query);

    AdminAccountVO getAccount(Long accountId);

    void changeStatus(Long accountId, Integer status);

    List<Long> roleIds(Long accountId);

    void assignRoles(Long accountId, List<Long> roleIds);
}
