package com.parking.system.dto;

import lombok.Data;

import java.util.List;

/**
 * 给角色分配菜单权限入参。
 * menuCodes 全量覆盖：传入 [] 表示清空该角色的菜单权限。
 */
@Data
public class RoleAssignMenusDTO {
    private List<String> menuCodes;
}
