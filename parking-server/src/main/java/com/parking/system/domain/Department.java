package com.parking.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学院 / 部门（校园组织结构，供注册与资料页选择）
 */
@Data
@TableName("t_department")
public class Department {
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    /** 学院/部门名称 */
    private String name;
    /** 学院代码，如 CS */
    private String code;
    /** 联系人 / 辅导员 */
    private String contact;
    /** 联系电话 */
    private String phone;
    private String remark;
    /** 1启用 0停用 */
    private Integer status;
    /** 排序号（越小越靠前） */
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
