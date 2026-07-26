package com.parking.controller;

import com.parking.common.R;
import com.parking.service.SpotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Spot", description = "Parking spot APIs")
@RestController
@RequestMapping("/api/spot")
@RequiredArgsConstructor
public class SpotController {

    private final SpotService spotService;

    @Operation(summary = "Get available parking spots")
    @GetMapping("/list")
    public R<?> list(@RequestParam(defaultValue = "1") int page,
                     @RequestParam(defaultValue = "20") int pageSize,
                     @RequestParam(required = false) String area) {
        return R.ok(spotService.getAvailableSpots(page, pageSize, area));
    }
}
