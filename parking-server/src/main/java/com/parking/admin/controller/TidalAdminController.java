package com.parking.admin.controller;

import com.parking.common.R;
import com.parking.common.annotation.OperationLog;
import com.parking.common.annotation.OperationType;
import com.parking.system.dto.TidalBatchDTO;
import com.parking.system.service.TidalSpotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端潮汐车位批次管理接口。
 */
@Tag(name = "TidalBatchAdmin", description = "潮汐车位批次管理 APIs")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class TidalAdminController {

    private final TidalSpotService tidalSpotService;

    @Operation(summary = "Tidal batch list")
    @GetMapping("/tidal-batches")
    public R<?> list(@RequestParam(defaultValue = "1") int page,
                     @RequestParam(defaultValue = "10") int pageSize,
                     @RequestParam(required = false) Integer status,
                     @RequestParam(required = false) String keyword) {
        return R.ok(tidalSpotService.pageForAdmin(page, pageSize, status, keyword));
    }

    @Operation(summary = "Tidal batch detail with spots")
    @GetMapping("/tidal-batch/{id}")
    public R<?> detail(@PathVariable Long id) {
        return R.ok(tidalSpotService.detailForAdmin(id));
    }

    @Operation(summary = "Create a tidal batch with generated spots")
    @PostMapping("/tidal-batch")
    @OperationLog(value = "创建潮汐批次", menuCode = "tidal", type = OperationType.CREATE)
    public R<?> create(@RequestBody TidalBatchDTO dto) {
        return R.ok(tidalSpotService.create(dto));
    }

    @Operation(summary = "Update a tidal batch (also syncs its spots)")
    @PutMapping("/tidal-batch/{id}")
    public R<?> update(@PathVariable Long id, @RequestBody TidalBatchDTO dto) {
        return R.ok(tidalSpotService.update(id, dto));
    }

    @Operation(summary = "Cancel a tidal batch (only when no vehicle on site and no active reservation)")
    @PutMapping("/tidal-batch/{id}/cancel")
    public R<?> cancel(@PathVariable Long id) {
        tidalSpotService.cancel(id);
        return R.ok();
    }

    @Operation(summary = "Delete a tidal batch (only when no reservation/vehicle history exists)")
    @DeleteMapping("/tidal-batch/{id}")
    public R<?> remove(@PathVariable Long id) {
        tidalSpotService.remove(id);
        return R.ok();
    }

    @Operation(summary = "Restore a cancelled tidal batch (un-lock and re-enable its spots)")
    @PutMapping("/tidal-batch/{id}/restore")
    @OperationLog(value = "恢复潮汐批次", menuCode = "tidal", type = OperationType.UPDATE)
    public R<?> restore(@PathVariable Long id) {
        tidalSpotService.restore(id);
        return R.ok();
    }

    @Operation(summary = "Soft-delete a tidal batch (hide from admin list, keep spots and history)")
    @PutMapping("/tidal-batch/{id}/soft-delete")
    @OperationLog(value = "软删除潮汐批次", menuCode = "tidal", type = OperationType.DELETE)
    public R<?> softDelete(@PathVariable Long id) {
        tidalSpotService.softDelete(id);
        return R.ok();
    }

    @Operation(summary = "Force-delete a tidal batch and all its spots (ignores reservation/vehicle history)")
    @DeleteMapping("/tidal-batch/{id}/force")
    @OperationLog(value = "彻底删除潮汐批次", menuCode = "tidal", type = OperationType.DELETE)
    public R<?> forceRemove(@PathVariable Long id) {
        tidalSpotService.forceRemove(id);
        return R.ok();
    }
}
