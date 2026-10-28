package com.parking.miniapp.controller;

import com.parking.common.R;
import com.parking.common.UserContext;
import com.parking.system.dto.SubscribeReportDTO;
import com.parking.system.service.SubscribeMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 微信订阅消息授权上报 & 模板信息接口。
 */
@Tag(name = "Subscribe", description = "WeChat Subscribe Message APIs")
@RestController
@RequestMapping("/api/user/subscribe")
@RequiredArgsConstructor
public class SubscribeController {

    private final SubscribeMessageService subscribeMessageService;

    @Operation(summary = "获取 4 类订阅消息场景对应的 templateId（前端 requestSubscribeMessage 时传入）")
    @GetMapping("/templates")
    public R<?> templates() {
        return R.ok(subscribeMessageService.templateMap());
    }

    @Operation(summary = "前端 requestSubscribeMessage 授权结果上报（同意/拒绝均需上报）")
    @PostMapping("/report")
    public R<?> report(@Valid @RequestBody SubscribeReportDTO dto) {
        subscribeMessageService.reportSubscription(UserContext.getUserId(), dto);
        return R.ok();
    }
}
