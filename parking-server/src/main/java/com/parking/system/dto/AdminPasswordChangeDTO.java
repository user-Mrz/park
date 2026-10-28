package com.parking.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员个人中心：修改自己的登录密码。
 * <p>与超管「重置他人密码」不同，这里必须校验原密码。</p>
 */
@Data
public class AdminPasswordChangeDTO {
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 50, message = "新密码长度需为 6-50 位")
    private String newPassword;
}
