package com.parking.system.dto;

import lombok.Data;

/**
 * 第三方校园管理系统单点认证请求体。
 * <p>由校园管理系统服务端调用，携带其权威身份信息，本系统据此完成免二次认证登录。</p>
 */
@Data
public class CampusSsoLoginDTO {

    /** 学号 / 工号（4-20 位字母或数字），作为校内人员的唯一标识 */
    private String userNo;

    /** 身份类型：1学生 2教师 3职工（仅校内人员可通过本接口对接） */
    private Integer identityType;

    /** 真实姓名（仅用于新建账号的默认昵称，已有自定义昵称不会被覆盖） */
    private String realName;

    /** 手机号（选填，仅在账号尚未绑定手机号时写入） */
    private String phone;

    /** 院系 / 部门 */
    private String department;

    /** 班级 */
    private String className;
}
