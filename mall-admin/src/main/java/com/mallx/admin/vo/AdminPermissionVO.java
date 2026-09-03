package com.mallx.admin.vo;

import lombok.*;

import java.time.*;

/** 代码定义的权限目录；不提供任意权限编码写入接口。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPermissionVO {
    /** 权限ID。 */
    private Long id;

    /** 权限编码。 */
    private String permissionCode;

    /** 名称。 */
    private String permissionName;

    /** 资源描述，不直接用于授权判断。 */
    private String resource;
}
