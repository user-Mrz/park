package com.parking.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 小程序用户端角色（RBAC）：角色 = 校园身份，用户账号绑定角色。
 * <p>与后台 t_role 完全分离，互不影响。</p>
 */
@Data
@TableName("t_user_role")
public class UserRole {
    @TableId(type = IdType.AUTO)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    /** 角色编码（唯一，t_user.role_code 存的就是该编码） */
    private String roleCode;
    /** 角色名称 */
    private String roleName;
    /** 对应的校园身份类型：1学生 2教师 3职工 4访客 5家长 */
    private Integer identityType;
    /** 角色描述 */
    private String description;
    /** 是否内置角色：1=是（不可删除），0=否 */
    private Integer builtin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
