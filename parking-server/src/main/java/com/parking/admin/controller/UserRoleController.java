package com.parking.admin.controller;

import com.parking.common.R;
import com.parking.common.annotation.OperationLog;
import com.parking.common.annotation.OperationType;
import com.parking.system.dto.UserRoleChangeDTO;
import com.parking.system.dto.UserRolePermDTO;
import com.parking.system.dto.UserRoleUpdateDTO;
import com.parking.system.service.UserRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 后台管理小程序用户端角色（RBAC）接口。
 * <p>
 * 用户角色与后台管理员角色完全分离：角色 = 校园身份，权限 = 可见停车区域。
 * 路径 /api/admin/user-roles、/api/admin/user-role/** 归属「用户管理」菜单（users）。
 */
@Tag(name = "UserRole", description = "小程序用户端角色（RBAC）管理 APIs")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class UserRoleController {

    private final UserRoleService userRoleService;

    @Operation(summary = "User role list with visible zones and user count")
    @GetMapping("/user-roles")
    public R<?> list(@RequestParam(required = false) String keyword) {
        Map<String, Object> data = new HashMap<>();
        data.put("records", userRoleService.listRoles(keyword));
        // 附带停车区域字典，供前端「可见区域」选择框使用
        data.put("zoneOptions", userRoleService.zoneOptions());
        return R.ok(data);
    }

    @Operation(summary = "Update user role (name / description)")
    @PutMapping("/user-role/{id}")
    @OperationLog(value = "修改用户角色", menuCode = "users", type = OperationType.UPDATE)
    public R<?> update(@PathVariable Long id, @RequestBody UserRoleUpdateDTO dto) {
        return R.ok(userRoleService.updateRole(id, dto));
    }

    @Operation(summary = "Get visible zones of user role")
    @GetMapping("/user-role/{id}/perms")
    public R<?> getPerms(@PathVariable Long id) {
        return R.ok(userRoleService.getRolePerms(id));
    }

    @Operation(summary = "Assign visible zones to user role (full overwrite)")
    @PutMapping("/user-role/{id}/perms")
    @OperationLog(value = "分配用户角色可见区域", menuCode = "users", type = OperationType.UPDATE)
    public R<?> assignPerms(@PathVariable Long id, @RequestBody UserRolePermDTO dto) {
        userRoleService.assignPerms(id, dto);
        return R.ok();
    }

    @Operation(summary = "Change the role of a user")
    @PutMapping("/user/{id}/role")
    @OperationLog(value = "调整用户角色", menuCode = "users", type = OperationType.UPDATE)
    public R<?> changeUserRole(@PathVariable Long id, @RequestBody UserRoleChangeDTO dto) {
        userRoleService.changeUserRole(id, dto == null ? null : dto.getRoleCode());
        return R.ok();
    }
}
