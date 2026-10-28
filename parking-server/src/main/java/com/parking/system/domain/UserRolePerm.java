package com.parking.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户角色-权限关联（RBAC）：某个角色可见的停车区域（zone_code）列表。
 * <p>决定该角色的用户在用户端能看到的停车场信息范围。</p>
 */
@Data
@TableName("t_user_role_perm")
public class UserRolePerm {
    @TableId(type = IdType.AUTO)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long roleId;
    /** 权限 code = 停车区域 zone_code（如 teaching/visitor/tidal） */
    private String permCode;
    private LocalDateTime createdAt;
}
