package com.parking.miniapp.controller;

import com.parking.common.R;
import com.parking.system.dto.AnnouncementAttachment;
import com.parking.system.service.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 小程序端公告接口（**无需登录**：公告属于面向全校的公开信息）。
 * 已发布公告列表 / 详情 / 附件下载。
 */
@Tag(name = "Announcement", description = "公告 APIs (public)")
@RestController
@RequestMapping("/api/announcement")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @Operation(summary = "Published announcement list (paged)")
    @GetMapping("/list")
    public R<?> list(@RequestParam(defaultValue = "1") int page,
                     @RequestParam(defaultValue = "20") int pageSize,
                     @RequestParam(required = false) String keyword) {
        return R.ok(announcementService.pageForUser(page, pageSize, keyword));
    }

    @Operation(summary = "Published announcement detail (increases view count)")
    @GetMapping("/{id}")
    public R<?> detail(@PathVariable Long id) {
        return R.ok(announcementService.detailForUser(id));
    }

    /**
     * 附件下载：以原始文件名返回，便于小程序下载后用 openDocument 打开。
     * 附件本身也放在 /uploads/** 静态目录下，这里额外提供一个带正确文件名的下载入口。
     */
    @Operation(summary = "Download announcement attachment with original filename")
    @GetMapping("/{id}/attachment")
    public ResponseEntity<Resource> attachment(@PathVariable Long id) {
        AnnouncementAttachment att = announcementService.attachmentOf(id);
        Resource resource = new FileSystemResource(att.getPath());

        String encoded;
        try {
            encoded = URLEncoder.encode(att.getFileName(), StandardCharsets.UTF_8.name()).replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            encoded = "attachment";
        }
        String asciiFallback = att.getFileName().replaceAll("[^\\x20-\\x7E]", "_").replace("\"", "_");

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        try {
            MediaType parsed = mediaTypeOf(att.getExt());
            if (parsed != null) mediaType = parsed;
        } catch (Exception ignore) {
            // 保持 octet-stream
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + asciiFallback + "\"; filename*=UTF-8''" + encoded)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .contentType(mediaType)
                .contentLength(att.getSize())
                .body(resource);
    }

    /** 附件类型 -> Content-Type */
    private MediaType mediaTypeOf(String ext) {
        if (ext == null) return null;
        switch (ext.toLowerCase()) {
            case "pdf": return MediaType.APPLICATION_PDF;
            case "txt": return MediaType.TEXT_PLAIN;
            case "jpg":
            case "jpeg": return MediaType.IMAGE_JPEG;
            case "png": return MediaType.IMAGE_PNG;
            case "gif": return MediaType.IMAGE_GIF;
            case "webp": return MediaType.parseMediaType("image/webp");
            case "bmp": return MediaType.parseMediaType("image/bmp");
            case "doc": return MediaType.parseMediaType("application/msword");
            case "docx": return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            case "xls": return MediaType.parseMediaType("application/vnd.ms-excel");
            case "xlsx": return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            case "ppt": return MediaType.parseMediaType("application/vnd.ms-powerpoint");
            case "pptx": return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.presentationml.presentation");
            case "zip": return MediaType.parseMediaType("application/zip");
            case "rar": return MediaType.parseMediaType("application/vnd.rar");
            case "7z": return MediaType.parseMediaType("application/x-7z-compressed");
            default: return null;
        }
    }

    /** 附件允许的扩展名（供前端提示，避免两端硬编码不一致） */
    @Operation(summary = "Allowed attachment extensions")
    @GetMapping("/attachment-types")
    public R<?> attachmentTypes() {
        Map<String, Object> data = new HashMap<>();
        data.put("extensions", com.parking.system.service.impl.AnnouncementServiceImpl.allowedExtensions());
        data.put("maxSizeMb", 20);
        return R.ok(data);
    }
}
