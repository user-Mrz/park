package com.parking.system.dto;

import lombok.Data;

import java.nio.file.Path;

/** 公告附件的下载描述（供控制器流式返回） */
@Data
public class AnnouncementAttachment {
    /** 附件在磁盘上的绝对路径 */
    private Path path;
    /** 附件原始文件名（用于 Content-Disposition） */
    private String fileName;
    /** 扩展名（小写，不含点） */
    private String ext;
    private long size;
}
