package com.parking.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parking.common.R;
import com.parking.system.dto.AdminLoginLogQueryDTO;
import com.parking.system.domain.AdminLoginLog;
import com.parking.system.mapper.AdminLoginLogMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 管理员登录日志查询接口（超管专用）。
 */
@Tag(name = "AdminLoginLog", description = "管理员登录日志 APIs")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminLoginLogController {

    private final AdminLoginLogMapper adminLoginLogMapper;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Operation(summary = "Admin login logs (paged)")
    @GetMapping("/login-logs")
    public R<?> list(@RequestParam(defaultValue = "1") int page,
                     @RequestParam(defaultValue = "10") int pageSize,
                     @RequestParam(required = false) String username,
                     @RequestParam(required = false) String loginStatus,
                     @RequestParam(required = false) String startTime,
                     @RequestParam(required = false) String endTime) {
        LambdaQueryWrapper<AdminLoginLog> w = new LambdaQueryWrapper<>();
        AdminLoginLogQueryDTO dto = new AdminLoginLogQueryDTO();
        dto.setUsername(username);
        dto.setLoginStatus(loginStatus);
        dto.setStartTime(startTime);
        dto.setEndTime(endTime);
        if (StringUtils.hasText(dto.getUsername())) {
            w.like(AdminLoginLog::getUsername, dto.getUsername());
        }
        if (StringUtils.hasText(dto.getLoginStatus())) {
            w.eq(AdminLoginLog::getLoginStatus, dto.getLoginStatus());
        }
        if (StringUtils.hasText(dto.getStartTime())) {
            try {
                w.ge(AdminLoginLog::getLoginTime, LocalDateTime.parse(dto.getStartTime(), DT));
            } catch (Exception ignore) {
                // 解析失败忽略
            }
        }
        if (StringUtils.hasText(dto.getEndTime())) {
            try {
                w.le(AdminLoginLog::getLoginTime, LocalDateTime.parse(dto.getEndTime(), DT));
            } catch (Exception ignore) {
                // 解析失败忽略
            }
        }
        w.orderByDesc(AdminLoginLog::getLoginTime);
        Page<AdminLoginLog> p = adminLoginLogMapper.selectPage(new Page<>(page, pageSize), w);
        return R.ok(p);
    }

    @Operation(summary = "Admin login log detail by id")
    @GetMapping("/login-log/{id}")
    public R<?> detail(@PathVariable Long id) {
        AdminLoginLog log = adminLoginLogMapper.selectById(id);
        if (log == null) {
            return R.fail("登录日志不存在");
        }
        return R.ok(log);
    }
}
