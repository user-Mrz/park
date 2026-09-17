package com.parking.controller;

import com.parking.common.R;
import com.parking.entity.Department;
import com.parking.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 学院/部门公开接口：注册与账号信息页的下拉数据（无需登录）
 */
@Tag(name = "Department", description = "Campus department APIs")
@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @Operation(summary = "Get enabled campus departments (public, for register/profile dropdown)")
    @GetMapping
    public R<?> list() {
        return R.ok(departmentService.list(true));
    }

    @Operation(summary = "Get department detail")
    @GetMapping("/{id}")
    public R<?> detail(@PathVariable Long id) {
        return R.ok(departmentService.list(false).stream()
                .filter(d -> d.getId().equals(id)).findFirst().orElse(null));
    }
}
