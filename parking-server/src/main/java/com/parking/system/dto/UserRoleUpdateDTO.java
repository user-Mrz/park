package com.parking.system.dto;

import lombok.Data;

/**
 * 后台更新用户角色名称/描述的请求体。角色编码与对应身份类型不可修改。
 */
@Data
public class UserRoleUpdateDTO {
    private String roleName;
    private String description;
}
