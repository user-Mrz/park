package com.parking.controller;

import com.parking.common.R;
import com.parking.common.UserContext;
import com.parking.dto.VehicleEnterDTO;
import com.parking.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Vehicle", description = "Vehicle entry/exit APIs")
@RestController
@RequestMapping("/api/vehicle")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @Operation(summary = "Record vehicle entry")
    @PostMapping("/enter")
    public R<?> enter(@Valid @RequestBody VehicleEnterDTO dto) {
        return R.ok(vehicleService.enter(UserContext.getUserId(), dto));
    }

    @Operation(summary = "Record vehicle exit (auto-pay)")
    @PostMapping("/exit/{recordId}")
    public R<?> exit(@PathVariable Long recordId) {
        return R.ok(vehicleService.exit(UserContext.getUserId(), recordId));
    }

    @Operation(summary = "Get current parking record")
    @GetMapping("/current")
    public R<?> current() {
        return R.ok(vehicleService.getCurrentParking(UserContext.getUserId()));
    }
}
