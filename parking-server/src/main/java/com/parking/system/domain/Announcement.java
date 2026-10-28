package com.parking.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告：由管理员发布的通知，可携带一个附件。
 *
 * <p>附件文件存放在 {@code ${parking.upload-dir}/announcement/} 目录下，
 * 数据库只保存存储文件名（attachmentPath）与原始文件名（attachmentName），
 * 访问路径由 VO 拼成 {@code /uploads/announcement/{attachmentPath}}。</p>
 */
@Data
@TableName("t_announcement")
public class Announcement {

    /** 状态：草稿 */
    public static final int STATUS_DRAFT = 0;
    /** 状态：已发布 */
    public static final int STATUS_PUBLISHED = 1;

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String title;
    private String content;

    /** 附件原始文件名（含扩展名，展示与下载时使用） */
    private String attachmentName;
    /** 附件在 uploads/announcement 下的存储文件名 */
    private String attachmentPath;
    private Long attachmentSize;
    /** 附件扩展名（小写，不含点） */
    private String attachmentExt;

    private Integer status;
    private String publisher;
    private LocalDateTime publishTime;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
