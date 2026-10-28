package com.parking.admin.controller;

import com.parking.common.R;
import com.parking.common.annotation.OperationLog;
import com.parking.common.annotation.OperationType;
import com.parking.system.dto.ParkingZoneDTO;
import com.parking.admin.service.ParkingZoneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 校园停车区域管理接口（归属「车位管理」菜单）。
 */
@Tag(name = "ParkingZone", description = "校园停车区域管理 APIs")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class ParkingZoneController {

    private final ParkingZoneService parkingZoneService;

    @Operation(summary = "Zone list with spot counts")
    @GetMapping("/zones")
    public R<?> list(@RequestParam(required = false) Integer status) {
        return R.ok(parkingZoneService.listAll(status));
    }

    @Operation(summary = "Create parking zone")
    @PostMapping("/zone")
    @OperationLog(value = "新增停车区域", menuCode = "spots", type = OperationType.CREATE)
    public R<?> create(@Valid @RequestBody ParkingZoneDTO dto) {
        return R.ok(parkingZoneService.create(dto));
    }

    @Operation(summary = "Update parking zone (name / description / sort)")
    @PutMapping("/zone/{id}")
    @OperationLog(value = "修改停车区域", menuCode = "spots", type = OperationType.UPDATE)
    public R<?> update(@PathVariable Long id, @Valid @RequestBody ParkingZoneDTO dto) {
        return R.ok(parkingZoneService.update(id, dto));
    }

    @Operation(summary = "Enable / disable parking zone")
    @PutMapping("/zone/{id}/status")
    @OperationLog(value = "启停停车区域", menuCode = "spots", type = OperationType.UPDATE)
    public R<?> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        parkingZoneService.updateStatus(id, status);
        return R.ok();
    }

    @Operation(summary = "Delete parking zone (only when no spot belongs to it)")
    @DeleteMapping("/zone/{id}")
    @OperationLog(value = "删除停车区域", menuCode = "spots", type = OperationType.DELETE)
    public R<?> delete(@PathVariable Long id) {
        parkingZoneService.delete(id);
        return R.ok();
    }
}
