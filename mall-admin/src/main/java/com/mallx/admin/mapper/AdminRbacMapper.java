package com.mallx.admin.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;

/** RBAC关联表没有单一主键，短固定SQL直接写在Mapper；复杂统计另放XML。 */
@Mapper
public interface AdminRbacMapper {
    /** 权限管理是低频操作，以保留角色行串行化角色删除与关联替换，避免悬空关联。 */
    @Select("SELECT id FROM auth_role WHERE role_code = 'SYSTEM_ADMIN' FOR UPDATE")
    Long lockSystemRole();

    @Select("SELECT role_id FROM auth_account_role WHERE account_id = #{accountId} ORDER BY role_id")
    List<Long> selectAccountRoleIds(@Param("accountId") Long accountId);

    @Select("SELECT permission_id FROM auth_role_permission WHERE role_id = #{roleId} ORDER BY permission_id")
    List<Long> selectRolePermissionIds(@Param("roleId") Long roleId);

    @Select("SELECT COUNT(*) FROM auth_account_role WHERE role_id = #{roleId}")
    long countRoleAccounts(@Param("roleId") Long roleId);

    @Delete("DELETE FROM auth_account_role WHERE account_id = #{accountId}")
    void deleteAccountRoles(@Param("accountId") Long accountId);

    @Insert("INSERT INTO auth_account_role(account_id, role_id) VALUES(#{accountId}, #{roleId})")
    void insertAccountRole(@Param("accountId") Long accountId, @Param("roleId") Long roleId);

    @Delete("DELETE FROM auth_role_permission WHERE role_id = #{roleId}")
    void deleteRolePermissions(@Param("roleId") Long roleId);

    @Insert("INSERT INTO auth_role_permission(role_id, permission_id) VALUES(#{roleId},#{permissionId})")
    void insertRolePermission(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);

}
