package com.parking.system.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parking.common.BusinessException;
import com.parking.system.dto.AnnouncementAttachment;
import com.parking.system.dto.AnnouncementVO;
import com.parking.system.domain.Announcement;
import com.parking.system.mapper.AnnouncementMapper;
import com.parking.system.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 公告实现。
 *
 * <p>附件存放在 {@code ${parking.upload-dir}/announcement/}，磁盘文件名为雪花ID + 扩展名
 * （纯 ASCII，避免中文文件名在 URL/下载头里出现编码问题），原始文件名单独存在数据库里，
 * 下载时通过 Content-Disposition 还原。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnnouncementServiceImpl implements AnnouncementService {

    /** 附件子目录 */
    private static final String SUB_DIR = "announcement";
    /** 静态访问前缀，与 WebConfig 暴露的 /uploads/** 对应 */
    private static final String URL_PREFIX = "/uploads/" + SUB_DIR + "/";
    /** 下载接口前缀 */
    private static final String DOWNLOAD_PREFIX = "/api/announcement/";

    /** 单个附件大小上限：20MB */
    private static final long MAX_ATTACHMENT_SIZE = 20L * 1024 * 1024;

    /**
     * 允许的附件类型白名单。
     * 刻意排除 html / htm / js / svg / xml 等可被浏览器执行或渲染的脚本类文件，
     * 避免上传目录被用作 XSS 载体（附件是静态目录直接对外暴露的）。
     */
    private static final Set<String> ALLOWED_EXT = new HashSet<>(Arrays.asList(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "txt", "zip", "rar", "7z",
            "jpg", "jpeg", "png", "gif", "webp", "bmp"));

    private final AnnouncementMapper announcementMapper;

    @Value("${parking.upload-dir:./uploads}")
    private String uploadDir;

    // ===================== 管理端 =====================

    @Override
    public Page<AnnouncementVO> pageForAdmin(int page, int pageSize, String keyword, Integer status) {
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(Announcement::getStatus, status);
        applyKeyword(wrapper, keyword);
        wrapper.orderByDesc(Announcement::getCreatedAt);
        Page<Announcement> p = announcementMapper.selectPage(new Page<>(page, pageSize), wrapper);
        Page<AnnouncementVO> out = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        out.setRecords(p.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return out;
    }

    @Override
    public AnnouncementVO detailForAdmin(Long id) {
        return toVO(require(id));
    }

    @Override
    @Transactional
    public AnnouncementVO create(String title, String content, Integer status, MultipartFile file, String publisher) {
        String t = validateTitle(title);
        String c = validateContent(content);
        int st = normalizeStatus(status);

        Announcement entity = new Announcement();
        entity.setId(IdUtil.getSnowflake(1, 10).nextId());
        entity.setTitle(t);
        entity.setContent(c);
        entity.setStatus(st);
        entity.setPublisher(StringUtils.hasText(publisher) ? publisher.trim() : "管理员");
        entity.setViewCount(0);
        entity.setAttachmentName("");
        entity.setAttachmentPath("");
        entity.setAttachmentSize(0L);
        entity.setAttachmentExt("");
        if (st == Announcement.STATUS_PUBLISHED) {
            entity.setPublishTime(LocalDateTime.now().withNano(0));
        }

        if (file != null && !file.isEmpty()) {
            applyAttachment(entity, file);
        }
        announcementMapper.insert(entity);
        log.info("Created announcement {} (status={}, attachment={})",
                entity.getId(), st, StringUtils.hasText(entity.getAttachmentPath()) ? entity.getAttachmentName() : "none");
        return toVO(entity);
    }

    @Override
    @Transactional
    public AnnouncementVO update(Long id, String title, String content, Integer status,
                                 MultipartFile file, Boolean removeAttachment) {
        Announcement entity = require(id);
        entity.setTitle(validateTitle(title));
        entity.setContent(validateContent(content));
        int st = normalizeStatus(status);

        String oldPath = entity.getAttachmentPath();
        if (file != null && !file.isEmpty()) {
            // 替换附件：先落新文件，成功后再删旧文件
            applyAttachment(entity, file);
            deleteQuietly(oldPath);
        } else if (Boolean.TRUE.equals(removeAttachment)) {
            entity.setAttachmentName("");
            entity.setAttachmentPath("");
            entity.setAttachmentSize(0L);
            entity.setAttachmentExt("");
            deleteQuietly(oldPath);
        }

        // 首次发布时记录发布时间；已发布过的公告再次保存不覆盖原发布时间
        if (st == Announcement.STATUS_PUBLISHED && entity.getPublishTime() == null) {
            entity.setPublishTime(LocalDateTime.now().withNano(0));
        }
        entity.setStatus(st);
        announcementMapper.updateById(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        Announcement entity = require(id);
        int st = normalizeStatus(status);
        entity.setStatus(st);
        if (st == Announcement.STATUS_PUBLISHED && entity.getPublishTime() == null) {
            entity.setPublishTime(LocalDateTime.now().withNano(0));
        }
        announcementMapper.updateById(entity);
    }

    @Override
    @Transactional
    public void remove(Long id) {
        Announcement entity = require(id);
        deleteQuietly(entity.getAttachmentPath());
        announcementMapper.deleteById(id);
        log.info("Deleted announcement {} (attachment removed)", id);
    }

    // ===================== 小程序端 =====================

    @Override
    public Page<AnnouncementVO> pageForUser(int page, int pageSize, String keyword) {
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getStatus, Announcement.STATUS_PUBLISHED);
        applyKeyword(wrapper, keyword);
        wrapper.orderByDesc(Announcement::getPublishTime).orderByDesc(Announcement::getCreatedAt);
        Page<Announcement> p = announcementMapper.selectPage(new Page<>(page, pageSize), wrapper);
        Page<AnnouncementVO> out = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        out.setRecords(p.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return out;
    }

    @Override
    @Transactional
    public AnnouncementVO detailForUser(Long id) {
        Announcement entity = announcementMapper.selectById(id);
        if (entity == null || entity.getStatus() == null
                || entity.getStatus() != Announcement.STATUS_PUBLISHED) {
            throw new BusinessException("公告不存在或已下架");
        }
        // 原子自增，避免并发下丢计数
        announcementMapper.update(null, new LambdaUpdateWrapper<Announcement>()
                .eq(Announcement::getId, id)
                .setSql("view_count = view_count + 1"));
        entity.setViewCount((entity.getViewCount() == null ? 0 : entity.getViewCount()) + 1);
        return toVO(entity);
    }

    @Override
    public AnnouncementAttachment attachmentOf(Long id) {
        Announcement entity = announcementMapper.selectById(id);
        if (entity == null) throw new BusinessException("公告不存在");
        if (!StringUtils.hasText(entity.getAttachmentPath())) {
            throw new BusinessException("该公告没有附件");
        }
        Path path = Paths.get(uploadDir, SUB_DIR).toAbsolutePath().normalize()
                .resolve(entity.getAttachmentPath()).normalize();
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new BusinessException("附件文件不存在或已被删除");
        }
        AnnouncementAttachment att = new AnnouncementAttachment();
        att.setPath(path);
        att.setFileName(StringUtils.hasText(entity.getAttachmentName())
                ? entity.getAttachmentName() : entity.getAttachmentPath());
        att.setExt(entity.getAttachmentExt());
        try {
            att.setSize(Files.size(path));
        } catch (IOException e) {
            att.setSize(entity.getAttachmentSize() == null ? 0L : entity.getAttachmentSize());
        }
        return att;
    }

    // ===================== 内部工具 =====================

    private void applyKeyword(LambdaQueryWrapper<Announcement> wrapper, String keyword) {
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Announcement::getTitle, kw).or().like(Announcement::getContent, kw));
        }
    }

    private Announcement require(Long id) {
        Announcement entity = id == null ? null : announcementMapper.selectById(id);
        if (entity == null) throw new BusinessException("公告不存在");
        return entity;
    }

    private String validateTitle(String title) {
        if (!StringUtils.hasText(title)) throw new BusinessException("请填写公告标题");
        String t = title.trim();
        if (t.length() > 128) throw new BusinessException("公告标题不能超过 128 个字符");
        return t;
    }

    private String validateContent(String content) {
        if (!StringUtils.hasText(content)) throw new BusinessException("请填写公告内容");
        return content.trim();
    }

    private int normalizeStatus(Integer status) {
        int st = status == null ? Announcement.STATUS_DRAFT : status;
        if (st != Announcement.STATUS_DRAFT && st != Announcement.STATUS_PUBLISHED) {
            throw new BusinessException("公告状态取值不正确");
        }
        return st;
    }

    /** 校验并保存附件，同时写入实体的附件字段 */
    private void applyAttachment(Announcement entity, MultipartFile file) {
        if (file.getSize() > MAX_ATTACHMENT_SIZE) {
            throw new BusinessException("附件大小不能超过 20MB");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        // 去掉可能夹带的路径，只保留文件名
        original = original.replace('\\', '/');
        int slash = original.lastIndexOf('/');
        if (slash >= 0) original = original.substring(slash + 1);
        if (!StringUtils.hasText(original)) original = "attachment";

        int dot = original.lastIndexOf('.');
        String ext = dot > -1 ? original.substring(dot + 1).toLowerCase() : "";
        if (!StringUtils.hasText(ext) || !ALLOWED_EXT.contains(ext)) {
            throw new BusinessException("附件格式不支持，仅允许：" + String.join(" / ", ALLOWED_EXT));
        }
        if (original.length() > 255) {
            original = original.substring(0, 200) + "." + ext;
        }

        try {
            Path dir = Paths.get(uploadDir, SUB_DIR).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            String stored = IdUtil.getSnowflake(1, 11).nextId() + "." + ext;
            Path target = dir.resolve(stored).normalize();
            // 防御路径穿越：最终路径必须仍在附件目录内
            if (!target.startsWith(dir)) throw new BusinessException("附件保存路径非法");
            file.transferTo(target.toFile());

            entity.setAttachmentName(original);
            entity.setAttachmentPath(stored);
            entity.setAttachmentSize(file.getSize());
            entity.setAttachmentExt(ext);
        } catch (IOException e) {
            throw new BusinessException("附件上传失败：" + e.getMessage());
        }
    }

    private void deleteQuietly(String storedName) {
        if (!StringUtils.hasText(storedName)) return;
        try {
            Path dir = Paths.get(uploadDir, SUB_DIR).toAbsolutePath().normalize();
            Path target = dir.resolve(storedName).normalize();
            if (target.startsWith(dir)) {
                Files.deleteIfExists(target);
            }
        } catch (IOException e) {
            log.warn("Failed to delete announcement attachment {}: {}", storedName, e.getMessage());
        }
    }

    private AnnouncementVO toVO(Announcement a) {
        AnnouncementVO vo = new AnnouncementVO();
        vo.setId(a.getId());
        vo.setTitle(a.getTitle());
        vo.setContent(a.getContent());
        vo.setAttachmentName(a.getAttachmentName() == null ? "" : a.getAttachmentName());
        vo.setAttachmentSize(a.getAttachmentSize() == null ? 0L : a.getAttachmentSize());
        vo.setAttachmentExt(a.getAttachmentExt() == null ? "" : a.getAttachmentExt());
        boolean hasAttachment = StringUtils.hasText(a.getAttachmentPath());
        vo.setAttachmentUrl(hasAttachment ? URL_PREFIX + a.getAttachmentPath() : "");
        vo.setAttachmentDownloadUrl(hasAttachment ? DOWNLOAD_PREFIX + a.getId() + "/attachment" : "");
        vo.setStatus(a.getStatus());
        vo.setStatusLabel(a.getStatus() != null && a.getStatus() == Announcement.STATUS_PUBLISHED ? "已发布" : "草稿");
        vo.setPublisher(a.getPublisher());
        vo.setPublishTime(a.getPublishTime());
        vo.setViewCount(a.getViewCount() == null ? 0 : a.getViewCount());
        vo.setCreatedAt(a.getCreatedAt());
        vo.setUpdatedAt(a.getUpdatedAt());
        return vo;
    }

    /** 供其它模块参考的允许扩展名列表 */
    public static List<String> allowedExtensions() {
        return ALLOWED_EXT.stream().sorted().collect(Collectors.toList());
    }
}
