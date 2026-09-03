package com.mallx.admin.dto;

import jakarta.validation.constraints.*;

import lombok.Data;

/** 创建或修改角色；系统保留角色不允许通过API修改。 */
@Data
public class RoleRequest {
    @NotBlank(message = "角色编码不能为空")
    @Pattern(regexp = "[A-Z][A-Z0-9_]{1,63}")
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 64)
    private String roleName;

    @NotNull(message = "角色状态不能为空")
    @Min(0)
    @Max(1)
    private Integer status;
}
