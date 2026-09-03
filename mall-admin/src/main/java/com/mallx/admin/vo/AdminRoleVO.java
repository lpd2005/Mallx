package com.mallx.admin.vo;

import lombok.*;

import java.time.*;

/** 角色管理视图。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRoleVO {
    /** 角色ID。 */
    private Long id;

    /** 唯一角色编码。 */
    private String roleCode;

    /** 展示名称。 */
    private String roleName;

    /** 0禁用、1启用。 */
    private Integer status;
}
