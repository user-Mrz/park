package com.parking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WechatLoginDTO {
    @NotBlank(message = "Code is required")
    private String code;

    private String nickname;
}
