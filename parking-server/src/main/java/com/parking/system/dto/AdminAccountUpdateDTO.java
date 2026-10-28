package com.parking.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 超管修改普通管理员账号信息入参（不含密码）。
 * <p>
 * roleCode 为空表示不调整角色；超级管理员账号不允许变更角色。
 */
@Data
public class AdminAccountUpdateDTO {
    @NotBlank(message = "昵称不能为空")
    @Size(max = 50)
    private String nickname;

    /** RBAC 角色编码（t_role.role_code） */
    private String roleCode;
}
