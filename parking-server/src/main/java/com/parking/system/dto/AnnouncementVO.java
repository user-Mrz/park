package com.parking.system.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告响应对象（管理端与小程序端共用）。
 *
 * <p>附件相关字段同时给出三种形式：原始文件名、静态访问路径、带原始文件名的下载接口路径。</p>
 */
@Data
public class AnnouncementVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String title;
    private String content;

    /** 附件原始文件名；为空表示没有附件 */
    private String attachmentName;
    /** 静态访问路径，形如 /uploads/announcement/123.pdf（无附件时为空） */
    private String attachmentUrl;
    /** 下载接口路径，形如 /api/announcement/1/attachment（无附件时为空，会带上原始文件名） */
    private String attachmentDownloadUrl;
    private Long attachmentSize;
    /** 扩展名（小写，不含点） */
    private String attachmentExt;

    private Integer status;
    private String statusLabel;
    private String publisher;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime publishTime;
    private Integer viewCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime updatedAt;
}
