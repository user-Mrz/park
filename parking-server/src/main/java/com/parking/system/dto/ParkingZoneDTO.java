package com.parking.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增 / 修改停车区域入参。
 * <p>zoneCode 仅新增时生效，修改时忽略（车位通过该编码归属区域，不允许变更）。</p>
 */
@Data
public class ParkingZoneDTO {
    @NotBlank(message = "区域编码不能为空")
    @Pattern(regexp = "^[a-z][a-z0-9_-]{1,31}$",
            message = "区域编码需以小写字母开头，仅可包含小写字母、数字、下划线和连字符，长度 2-32")
    private String zoneCode;

    @NotBlank(message = "区域名称不能为空")
    @Size(max = 64, message = "区域名称不能超过 64 个字符")
    private String zoneName;

    @Size(max = 255, message = "区域说明不能超过 255 个字符")
    private String description;

    /** 展示顺序，越小越靠前；不填按 0 处理 */
    private Integer sortOrder;
}
