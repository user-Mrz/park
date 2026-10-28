package com.parking.miniapp.controller;

import com.parking.common.R;
import com.parking.common.UserContext;
import com.parking.system.dto.MonthlyPassOpenDTO;
import com.parking.miniapp.service.MonthlyPassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 月卡 / 长租停车权益接口。
 */
@Tag(name = "MonthlyPass", description = "月卡 / 长租停车权益 APIs")
@RestController
@RequestMapping("/api/user/monthly-pass")
@RequiredArgsConstructor
public class MonthlyPassController {

    private final MonthlyPassService monthlyPassService;

    @Operation(summary = "开通 / 续费月卡")
    @PostMapping("/open")
    public R<?> open(@Valid @RequestBody MonthlyPassOpenDTO dto) {
        return R.ok(monthlyPassService.open(UserContext.getUserId(), dto));
    }

    @Operation(summary = "当前有效月卡")
    @GetMapping("/current")
    public R<?> current() {
        return R.ok(monthlyPassService.current(UserContext.getUserId()));
    }

    @Operation(summary = "我的月卡历史列表")
    @GetMapping("/list")
    public R<?> list(@RequestParam(defaultValue = "1") int page,
                     @RequestParam(defaultValue = "10") int pageSize) {
        return R.ok(monthlyPassService.listMy(UserContext.getUserId(), page, pageSize));
    }

    @Operation(summary = "月卡价格配置（月/季/年）")
    @GetMapping("/prices")
    public R<?> prices() {
        return R.ok(monthlyPassService.priceConfig());
    }
}
