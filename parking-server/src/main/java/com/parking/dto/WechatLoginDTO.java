package com.parking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WechatLoginDTO {
    @NotBlank(message = "Code is required")
    private String code;

    private String nickname;

    /**
     * 设备标识：开发模式下用于生成稳定的 mock openid；
     * 正式接入微信 code2session 后可不再依赖该字段。
     */
    private String deviceId;
}
