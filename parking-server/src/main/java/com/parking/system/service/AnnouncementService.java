package com.parking.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parking.system.dto.AnnouncementAttachment;
import com.parking.system.dto.AnnouncementVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 公告服务。
 *
 * <p>管理端可以发布带一个附件的公告（附件保存到上传目录的 announcement 子目录）；
 * 小程序端只能看到「已发布」状态的公告，并可浏览/下载附件。</p>
 */
public interface AnnouncementService {

    // ===================== 管理端 =====================

    /** 分页查询（含草稿），keyword 匹配标题与正文 */
    Page<AnnouncementVO> pageForAdmin(int page, int pageSize, String keyword, Integer status);

    AnnouncementVO detailForAdmin(Long id);

    /** 新建公告；file 可为空表示无附件。返回新建后的完整对象 */
    AnnouncementVO create(String title, String content, Integer status, MultipartFile file, String publisher);

    /**
     * 修改公告。
     *
     * @param file             新附件，可为空（为空表示保留原附件）
     * @param removeAttachment 为 true 时删除现有附件（file 为空时生效）
     */
    AnnouncementVO update(Long id, String title, String content, Integer status, MultipartFile file, Boolean removeAttachment);

    /** 发布 / 下架（status 1 发布，0 下架为草稿） */
    void updateStatus(Long id, Integer status);

    /** 删除公告并删除其附件文件 */
    void remove(Long id);

    // ===================== 小程序端 =====================

    /** 已发布公告分页 */
    Page<AnnouncementVO> pageForUser(int page, int pageSize, String keyword);

    /** 已发布公告详情（会累加浏览量）；草稿不可见 */
    AnnouncementVO detailForUser(Long id);

    /** 取附件（无附件或文件已丢失时抛出业务异常） */
    AnnouncementAttachment attachmentOf(Long id);
}
