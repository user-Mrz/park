package com.parking.system.dto;

import lombok.Data;

/**
 * 后台调整某个用户所属角色的请求体。
 */
@Data
public class UserRoleChangeDTO {
    /** 目标角色编码（t_user_role.role_code） */
    private String roleCode;
}
