package com.mallx.admin.vo;

import lombok.*;

import java.time.*;

/** 后台账号安全视图，永远不包含密码摘要。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAccountVO {
    /** 用户ID。 */
    private Long id;

    /** 仅用户管理权限可查看的登录手机号。 */
    private String phone;

    /** 0禁用、1启用。 */
    private Integer status;

    /** 注册时间。 */
    private LocalDateTime createdAt;
}
