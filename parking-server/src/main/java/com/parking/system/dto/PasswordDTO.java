package com.parking.system.dto;

import lombok.Data;

/**
 * 修改密码请求
 */
@Data
public class PasswordDTO {
    /** 原密码 */
    private String oldPassword;
    /** 新密码 */
    private String newPassword;
}
