package com.parking.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 超管创建普通管理员账号入参。
 */
@Data
public class AdminAccountCreateDTO {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 30, message = "用户名长度 3-30")
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "用户名只能包含字母、数字、下划线")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度 6-50")
    private String password;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 50)
    private String nickname;

    /** RBAC 角色编码（t_role.role_code），为空时默认使用 admin 角色 */
    private String roleCode;
}
