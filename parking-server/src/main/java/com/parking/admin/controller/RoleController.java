package com.parking.admin.controller;

import com.parking.common.R;
import com.parking.common.annotation.OperationLog;
import com.parking.common.annotation.OperationType;
import com.parking.common.menu.MenuPermissionMapping;
import com.parking.system.dto.RoleAssignMenusDTO;
import com.parking.system.dto.RoleSaveDTO;
import com.parking.admin.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 后台角色（RBAC）管理接口：仅 super_admin 可访问（由 MenuAuthInterceptor 强制 403）。
 * <p>
 * 路径 /api/admin/roles、/api/admin/role/** 归属「管理员账号管理」菜单（accounts，超管专属）。
 */
@Tag(name = "AdminRole", description = "后台角色（RBAC）管理 APIs")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "Role list with menus and account count")
    @GetMapping("/roles")
    public R<?> list(@RequestParam(required = false) String keyword) {
        Map<String, Object> data = new HashMap<>();
        data.put("records", roleService.listRoles(keyword));
        // 附带菜单字典，供前端「分配菜单」对话框使用
        data.put("allMenuCodes", MenuPermissionMapping.allMenuCodes());
        data.put("allMenuLabels", MenuPermissionMapping.allMenuLabels());
        data.put("superOnlyMenus", MenuPermissionMapping.superOnlyMenus());
        return R.ok(data);
    }

    @Operation(summary = "Create role")
    @PostMapping("/role")
    @OperationLog(value = "创建角色", menuCode = "accounts", type = OperationType.CREATE)
    public R<?> create(@RequestBody RoleSaveDTO dto) {
        return R.ok(roleService.create(dto));
    }

    @Operation(summary = "Update role (name / description)")
    @PutMapping("/role/{id}")
    @OperationLog(value = "修改角色", menuCode = "accounts", type = OperationType.UPDATE)
    public R<?> update(@PathVariable Long id, @RequestBody RoleSaveDTO dto) {
        return R.ok(roleService.update(id, dto));
    }

    @Operation(summary = "Delete role (builtin or in-use role is rejected)")
    @DeleteMapping("/role/{id}")
    @OperationLog(value = "删除角色", menuCode = "accounts", type = OperationType.DELETE)
    public R<?> delete(@PathVariable Long id) {
        roleService.delete(id);
        return R.ok();
    }

    @Operation(summary = "Get menus of role")
    @GetMapping("/role/{id}/menus")
    public R<?> getMenus(@PathVariable Long id) {
        return R.ok(roleService.getRoleMenus(id));
    }

    @Operation(summary = "Assign menus to role (full overwrite)")
    @PutMapping("/role/{id}/menus")
    @OperationLog(value = "分配角色菜单权限", menuCode = "accounts", type = OperationType.UPDATE)
    public R<?> assignMenus(@PathVariable Long id, @RequestBody RoleAssignMenusDTO dto) {
        roleService.assignMenus(id, dto);
        return R.ok();
    }
}
