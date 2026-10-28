package com.parking.system.dto;

import lombok.Data;

import java.util.List;

/**
 * 后台为用户角色分配可见停车区域（权限）的请求体：全量覆盖。
 */
@Data
public class UserRolePermDTO {
    /** 可见停车区域 zone_code 列表，如 ["visitor","tidal"] */
    private List<String> zoneCodes;
}
