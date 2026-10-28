package com.parking.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台角色（RBAC）：角色 -> 菜单权限集合，管理员账号绑定角色。
 */
@Data
@TableName("t_role")
public class Role {
    @TableId(type = IdType.AUTO)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    /** 角色编码（唯一，账号 t_admin.role 存的就是该编码） */
    private String roleCode;
    /** 角色名称 */
    private String roleName;
    /** 角色描述 */
    private String description;
    /** 是否内置角色：1=是（不可删除），0=否 */
    private Integer builtin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
