package com.parking.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员个人中心：修改自己的昵称。
 */
@Data
public class AdminProfileUpdateDTO {
    @NotBlank(message = "昵称不能为空")
    @Size(max = 32, message = "昵称不能超过 32 个字符")
    private String nickname;
}
