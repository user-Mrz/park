package com.parking.system.dto;

import lombok.Data;

/**
 * 角色新增/修改入参。
 * <p>
 * 新增时 roleCode 必填；修改时 roleCode 不可变更（会被忽略），仅更新名称与描述。
 */
@Data
public class RoleSaveDTO {
    private String roleCode;
    private String roleName;
    private String description;
}
