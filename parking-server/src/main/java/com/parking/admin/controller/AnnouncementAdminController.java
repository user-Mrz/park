package com.parking.admin.controller;

import com.parking.common.R;
import com.parking.common.UserContext;
import com.parking.common.annotation.OperationLog;
import com.parking.common.annotation.OperationType;
import com.parking.system.domain.Admin;
import com.parking.system.mapper.AdminMapper;
import com.parking.system.service.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 管理端公告管理接口：发布 / 编辑带附件的公告、上下架、删除。
 */
@Tag(name = "AnnouncementAdmin", description = "公告管理 APIs")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AnnouncementAdminController {

    private final AnnouncementService announcementService;
    private final AdminMapper adminMapper;

    @Operation(summary = "Announcement list (includes drafts)")
    @GetMapping("/announcements")
    public R<?> list(@RequestParam(defaultValue = "1") int page,
                     @RequestParam(defaultValue = "10") int pageSize,
                     @RequestParam(required = false) String keyword,
                     @RequestParam(required = false) Integer status) {
        return R.ok(announcementService.pageForAdmin(page, pageSize, keyword, status));
    }

    @Operation(summary = "Announcement detail")
    @GetMapping("/announcement/{id}")
    public R<?> detail(@PathVariable Long id) {
        return R.ok(announcementService.detailForAdmin(id));
    }

    @Operation(summary = "Create announcement (multipart; optional attachment field name = file)")
    @PostMapping(value = "/announcement", consumes = "multipart/form-data")
    @OperationLog(value = "发布公告", menuCode = "announcements", type = OperationType.CREATE)
    public R<?> create(@RequestParam String title,
                       @RequestParam String content,
                       @RequestParam(required = false) Integer status,
                       @RequestParam(value = "file", required = false) MultipartFile file) {
        return R.ok(announcementService.create(title, content, status, file, currentPublisher()));
    }

    @Operation(summary = "Update announcement (multipart; optional new file / removeAttachment)")
    @PutMapping(value = "/announcement/{id}", consumes = "multipart/form-data")
    public R<?> update(@PathVariable Long id,
                       @RequestParam String title,
                       @RequestParam String content,
                       @RequestParam(required = false) Integer status,
                       @RequestParam(value = "file", required = false) MultipartFile file,
                       @RequestParam(required = false) Boolean removeAttachment) {
        return R.ok(announcementService.update(id, title, content, status, file, removeAttachment));
    }

    @Operation(summary = "Publish / unpublish announcement (status 1 published, 0 draft)")
    @PutMapping("/announcement/{id}/status")
    public R<?> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        announcementService.updateStatus(id, status);
        return R.ok();
    }

    @Operation(summary = "Delete announcement (its attachment file is removed too)")
    @DeleteMapping("/announcement/{id}")
    public R<?> remove(@PathVariable Long id) {
        announcementService.remove(id);
        return R.ok();
    }

    /** 发布人取当前登录管理员的昵称，取不到时回落到「管理员」 */
    private String currentPublisher() {
        try {
            Long adminId = UserContext.getUserId();
            if (adminId != null) {
                Admin admin = adminMapper.selectById(adminId);
                if (admin != null) {
                    if (StringUtils.hasText(admin.getNickname())) return admin.getNickname().trim();
                    if (StringUtils.hasText(admin.getUsername())) return admin.getUsername().trim();
                }
            }
        } catch (Exception ignore) {
            // 发布人只是展示信息，取不到不影响发布
        }
        return "公告发布人";
    }
}
