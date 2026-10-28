package com.parking.admin.controller;

import com.parking.common.R;
import com.parking.system.dto.AdminPasswordChangeDTO;
import com.parking.system.dto.AdminProfileUpdateDTO;
import com.parking.admin.service.AdminAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员个人中心接口。
 * <p>
 * 与「管理员账号管理」不同：这里只操作当前登录者自己的资料与密码，
 * 任何角色（含普通管理员、车位管理员）都可访问，不受菜单权限限制
 * （见 MenuPermissionMapping.PUBLIC_PREFIXES 中的 /profile）。
 */
@Tag(name = "Profile", description = "管理员个人中心 APIs")
@RestController
@RequestMapping("/api/admin/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final AdminAccountService adminAccountService;

    @Operation(summary = "Get current admin profile")
    @GetMapping
    public R<?> myProfile() {
        return R.ok(adminAccountService.getMyProfile());
    }

    @Operation(summary = "Update current admin nickname")
    @PutMapping
    public R<?> updateNickname(@Valid @RequestBody AdminProfileUpdateDTO dto) {
        adminAccountService.updateMyNickname(dto.getNickname());
        return R.ok();
    }

    @Operation(summary = "Change current admin login password (requires old password)")
    @PutMapping("/password")
    public R<?> changePassword(@Valid @RequestBody AdminPasswordChangeDTO dto) {
        adminAccountService.changeMyPassword(dto.getOldPassword(), dto.getNewPassword());
        return R.ok();
    }
}
