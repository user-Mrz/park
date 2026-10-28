package com.parking.miniapp.controller;

import com.parking.common.R;
import com.parking.system.service.TidalSpotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端潮汐车位接口。
 *
 * <p>返回开放时间前 N 天（N = tidal_reserve_advance_days，默认 7 天）至结束时间之间的批次，
 * 每个批次带 phase：upcoming（仅可预约）/ open（可预约可入场）。</p>
 */
@Tag(name = "TidalSpot", description = "潮汐(临时)车位 APIs")
@RestController
@RequestMapping("/api/spot")
@RequiredArgsConstructor
public class TidalSpotController {

    private final TidalSpotService tidalSpotService;

    @Operation(summary = "Get visible tidal parking batches (from N days before openTime)")
    @GetMapping("/tidal")
    public R<?> tidal() {
        return R.ok(tidalSpotService.listForUser());
    }
}
