package com.parking.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterDTO {
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码至少 6 位")
    private String password;

    private String nickname;

    private String plateNumber;

    /** 学号 / 工号（学生、教师、职工必填，需管理员审核） */
    private String userNo;

    /** 1学生 2教师 3职工 4访客 5家长，默认 1 学生 */
    private Integer identityType;

    /** 院系 / 部门 */
    private String department;

    /** 班级 */
    private String className;

    @NotBlank(message = "验证码标识不能为空")
    private String captchaKey;

    @NotBlank(message = "验证码不能为空")
    private String captchaCode;
}
