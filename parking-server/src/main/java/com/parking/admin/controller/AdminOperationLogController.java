package com.parking.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parking.common.R;
import com.parking.system.dto.AdminOperationLogQueryDTO;
import com.parking.system.domain.AdminOperationLog;
import com.parking.system.mapper.AdminOperationLogMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 管理员操作日志查询接口（超管专用）。
 */
@Tag(name = "AdminOperationLog", description = "管理员操作日志 APIs")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminOperationLogController {

    private final AdminOperationLogMapper adminOperationLogMapper;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Operation(summary = "Admin operation logs (paged)")
    @GetMapping("/operation-logs")
    public R<?> list(@RequestParam(defaultValue = "1") int page,
                     @RequestParam(defaultValue = "10") int pageSize,
                     @RequestParam(required = false) String username,
                     @RequestParam(required = false) String menuCode,
                     @RequestParam(required = false) String type,
                     @RequestParam(required = false) String startTime,
                     @RequestParam(required = false) String endTime) {
        AdminOperationLogQueryDTO dto = new AdminOperationLogQueryDTO();
        dto.setUsername(username);
        dto.setMenuCode(menuCode);
        dto.setType(type);
        dto.setStartTime(startTime);
        dto.setEndTime(endTime);
        LambdaQueryWrapper<AdminOperationLog> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(dto.getUsername())) {
            w.like(AdminOperationLog::getUsername, dto.getUsername());
        }
        if (StringUtils.hasText(dto.getMenuCode())) {
            w.eq(AdminOperationLog::getMenuCode, dto.getMenuCode());
        }
        if (StringUtils.hasText(dto.getType())) {
            w.eq(AdminOperationLog::getType, dto.getType().toUpperCase());
        }
        if (StringUtils.hasText(dto.getStartTime())) {
            try {
                w.ge(AdminOperationLog::getOperationTime, LocalDateTime.parse(dto.getStartTime(), DT));
            } catch (Exception ignore) {
                // 解析失败忽略
            }
        }
        if (StringUtils.hasText(dto.getEndTime())) {
            try {
                w.le(AdminOperationLog::getOperationTime, LocalDateTime.parse(dto.getEndTime(), DT));
            } catch (Exception ignore) {
                // 解析失败忽略
            }
        }
        w.orderByDesc(AdminOperationLog::getOperationTime);
        Page<AdminOperationLog> p = adminOperationLogMapper.selectPage(new Page<>(page, pageSize), w);
        return R.ok(p);
    }

    @Operation(summary = "Admin operation log detail by id")
    @GetMapping("/operation-log/{id}")
    public R<?> detail(@PathVariable Long id) {
        AdminOperationLog log = adminOperationLogMapper.selectById(id);
        if (log == null) {
            return R.fail("操作日志不存在");
        }
        return R.ok(log);
    }
}
