package com.mallx.admin.vo;

import lombok.*;

import java.time.*;
import java.util.List;

/** 后台当前会话，可供前端控制菜单；不能替代后端鉴权。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSessionVO {
    /** 当前管理员ID。 */
    private Long userId;

    /** 启用角色编码。 */
    private List<String> roles;

    /** 当前有效权限。 */
    private List<String> permissions;
}
