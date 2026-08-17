package com.mallx.auth.config;

import cn.dev33.satoken.stp.StpInterface;
import com.mallx.auth.mapper.AuthPermissionMapper;
import com.mallx.auth.mapper.AuthRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Sa-Token 的 RBAC 数据桥接实现。
 *
 * <p>后续业务接口可通过 {@code @SaCheckRole}、{@code @SaCheckPermission}
 * 或 {@code StpUtil.checkRole/checkPermission} 使用这些角色与权限。</p>
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final AuthPermissionMapper authPermissionMapper;

    private final AuthRoleMapper authRoleMapper;

    /**
     * 返回登录账号的权限编码。
     *
     * @param loginId Sa-Token 登录身份
     * @param loginType 登录类型，MallX 当前仅使用默认登录类型
     * @return 权限编码列表
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long accountId = toAccountId(loginId);
        return accountId == null
                ? Collections.emptyList()
                : authPermissionMapper.selectPermissionCodesByAccountId(accountId);
    }

    /**
     * 返回登录账号的角色编码。
     *
     * @param loginId Sa-Token 登录身份
     * @param loginType 登录类型，MallX 当前仅使用默认登录类型
     * @return 角色编码列表
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long accountId = toAccountId(loginId);
        return accountId == null
                ? Collections.emptyList()
                : authRoleMapper.selectRoleCodesByAccountId(accountId);
    }

    /**
     * Sa-Token 的登录身份是 Object；无效值不查询数据库，直接视为无权限。
     */
    private Long toAccountId(Object loginId) {
        if (loginId == null) {
            return null;
        }
        try {
            return Long.valueOf(loginId.toString());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
