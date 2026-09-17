package com.parking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_user")
public class User {
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String openid;
    private String nickname;
    /** 学号 / 工号（访客账号可为空） */
    private String userNo;
    /** 1学生 2教师 3职工 4访客 5家长 */
    private Integer identityType;
    /** 院系 / 部门 */
    private String department;
    /** 班级 */
    private String className;
    /** 身份认证状态：0待审核 1已认证 2已驳回 */
    private Integer identityStatus;
    /** 身份审核备注 */
    private String identityRemark;
    private String phone;
    @JsonIgnore
    private String password;
    private String plateNumber;
    /** 头像访问路径，形如 /uploads/avatar/xxx.jpg */
    private String avatar;
    private BigDecimal balance;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
