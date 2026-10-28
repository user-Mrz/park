package com.parking.admin.controller;

import com.parking.admin.service.ScheduledJobService;
import com.parking.common.R;
import com.parking.common.annotation.OperationLog;
import com.parking.common.annotation.OperationType;
import com.parking.system.dto.ScheduledJobDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 定时任务管理接口：列表 / 新增 / 修改 / 删除 / 启停 / 立即执行。
 */
@Tag(name = "ScheduledJob", description = "定时任务管理 APIs")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class ScheduledJobController {

    private final ScheduledJobService scheduledJobService;

    @Operation(summary = "Scheduled job list (paged)")
    @GetMapping("/jobs")
    public R<?> list(@RequestParam(defaultValue = "1") int page,
                     @RequestParam(defaultValue = "10") int pageSize,
                     @RequestParam(required = false) String keyword,
                     @RequestParam(required = false) Integer status) {
        return R.ok(scheduledJobService.page(page, pageSize, keyword, status));
    }

    @Operation(summary = "Available job handlers (whitelist)")
    @GetMapping("/job/handlers")
    public R<?> handlers() {
        return R.ok(scheduledJobService.handlers());
    }

    @Operation(summary = "Create scheduled job")
    @PostMapping("/job")
    @OperationLog(value = "新增定时任务", menuCode = "jobs", type = OperationType.CREATE)
    public R<?> create(@RequestBody ScheduledJobDTO dto) {
        return R.ok(scheduledJobService.create(dto));
    }

    @Operation(summary = "Update scheduled job")
    @PutMapping("/job/{id}")
    @OperationLog(value = "修改定时任务", menuCode = "jobs", type = OperationType.UPDATE)
    public R<?> update(@PathVariable Long id, @RequestBody ScheduledJobDTO dto) {
        return R.ok(scheduledJobService.update(id, dto));
    }

    @Operation(summary = "Delete scheduled job")
    @DeleteMapping("/job/{id}")
    @OperationLog(value = "删除定时任务", menuCode = "jobs", type = OperationType.DELETE)
    public R<?> delete(@PathVariable Long id) {
        scheduledJobService.delete(id);
        return R.ok();
    }

    @Operation(summary = "Enable / disable scheduled job (0 pause / 1 run)")
    @PutMapping("/job/{id}/status")
    @OperationLog(value = "启停定时任务", menuCode = "jobs", type = OperationType.UPDATE)
    public R<?> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        scheduledJobService.changeStatus(id, status);
        return R.ok();
    }

    @Operation(summary = "Run scheduled job once (async)")
    @PostMapping("/job/{id}/run")
    @OperationLog(value = "立即执行定时任务", menuCode = "jobs", type = OperationType.UPDATE)
    public R<?> run(@PathVariable Long id) {
        scheduledJobService.runOnce(id);
        return R.ok();
    }
}
