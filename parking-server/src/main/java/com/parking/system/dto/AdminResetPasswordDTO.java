package com.parking.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 超管重置普通管理员密码入参。
 */
@Data
public class AdminResetPasswordDTO {
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度 6-50")
    private String newPassword;
}
