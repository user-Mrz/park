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

    @NotBlank(message = "Captcha key is required")
    private String captchaKey;

    @NotBlank(message = "Captcha code is required")
    private String captchaCode;
}
