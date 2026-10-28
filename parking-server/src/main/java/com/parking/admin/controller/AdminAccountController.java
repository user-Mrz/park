package com.parking.admin.controller;

import com.parking.common.R;
import com.parking.common.annotation.OperationLog;
import com.parking.common.annotation.OperationType;
import com.parking.system.dto.AdminAccountCreateDTO;
import com.parking.system.dto.AdminAccountUpdateDTO;
import com.parking.system.dto.AdminResetPasswordDTO;
import com.parking.system.domain.Admin;
import com.parking.admin.service.AdminAccountService;
import com.parking.admin.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 超管账号管理接口：仅 super_admin 可访问（由 MenuAuthInterceptor 强制 403）。
 * <p>
 * RBAC：账号绑定角色（t_role），菜单权限由角色决定；角色维护见 {@link RoleController}。
 */
@Tag(name = "AdminAccount", description = "管理员账号管理 APIs")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminAccountController {

    private final AdminAccountService adminAccountService;
    private final RoleService roleService;

    @Operation(summary = "Admin account list (paged)")
    @GetMapping("/accounts")
    public R<?> list(@RequestParam(defaultValue = "1") int page,
                     @RequestParam(defaultValue = "10") int pageSize,
                     @RequestParam(required = false) String keyword,
                     @RequestParam(required = false) Integer status) {
        var p = adminAccountService.listAccounts(page, pageSize, keyword, status);
        // 每行附上角色名称与其角色对应的菜单 code 列表（RBAC：菜单来自角色，不来自账号）
        Map<String, String> roleNames = roleService.roleNames();
        Map<String, List<String>> menusByRole = new HashMap<>();
        List<Map<String, Object>> rows = p.getRecords().stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", String.valueOf(a.getId()));
            m.put("username", a.getUsername());
            m.put("nickname", a.getNickname());
            m.put("role", a.getRole());
            m.put("roleName", roleNames.getOrDefault(a.getRole(), a.getRole()));
            m.put("status", a.getStatus());
            m.put("createdAt", a.getCreatedAt());
            m.put("menus", menusByRole.computeIfAbsent(a.getRole(), roleService::getMenusByRoleCode));
            return m;
        }).toList();
        Map<String, Object> data = new HashMap<>();
        data.put("records", rows);
        data.put("total", p.getTotal());
        data.put("current", p.getCurrent());
        data.put("size", p.getSize());
        return R.ok(data);
    }

    @Operation(summary = "Create admin account")
    @PostMapping("/account")
    @OperationLog(value = "创建管理员账号", menuCode = "accounts", type = OperationType.CREATE)
    public R<?> create(@Valid @RequestBody AdminAccountCreateDTO dto) {
        return R.ok(adminAccountService.create(dto));
    }

    @Operation(summary = "Update admin account (nickname / role)")
    @PutMapping("/account/{id}")
    @OperationLog(value = "修改管理员账号", menuCode = "accounts", type = OperationType.UPDATE)
    public R<?> update(@PathVariable Long id, @Valid @RequestBody AdminAccountUpdateDTO dto) {
        return R.ok(adminAccountService.update(id, dto));
    }

    @Operation(summary = "Delete admin account (cannot delete super_admin or self)")
    @DeleteMapping("/account/{id}")
    @OperationLog(value = "删除管理员账号", menuCode = "accounts", type = OperationType.DELETE)
    public R<?> delete(@PathVariable Long id) {
        adminAccountService.delete(id);
        return R.ok();
    }

    @Operation(summary = "Reset admin password")
    @PutMapping("/account/{id}/reset-password")
    @OperationLog(value = "重置管理员密码", menuCode = "accounts", type = OperationType.UPDATE)
    public R<?> resetPassword(@PathVariable Long id, @Valid @RequestBody AdminResetPasswordDTO dto) {
        adminAccountService.resetPassword(id, dto);
        return R.ok();
    }

    @Operation(summary = "Enable / disable admin account")
    @PutMapping("/account/{id}/status")
    @OperationLog(value = "启停管理员账号", menuCode = "accounts", type = OperationType.UPDATE)
    public R<?> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        adminAccountService.updateStatus(id, status);
        return R.ok();
    }

    @Operation(summary = "Get current admin's menus (called after login)")
    @GetMapping("/account/my-menus")
    public R<?> myMenus() {
        // 公共路径放行：普通管理员登录后立即调用
        return R.ok(adminAccountService.getMyMenus());
    }
}
