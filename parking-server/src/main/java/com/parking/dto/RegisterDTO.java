package com.parking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterDTO {
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "Invalid phone number format")
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
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

    @NotBlank(message = "Captcha key is required")
    private String captchaKey;

    @NotBlank(message = "Captcha code is required")
    private String captchaCode;
}
