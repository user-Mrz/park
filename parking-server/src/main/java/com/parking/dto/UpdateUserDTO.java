package com.parking.dto;

import lombok.Data;

/**
 * 用户资料更新请求（均为可选字段，只更新传入的字段）
 */
@Data
public class UpdateUserDTO {
    /** 昵称 */
    private String nickname;
    /** 手机号 */
    private String phone;
    /** 车牌号 */
    private String plateNumber;
    /** 头像地址（由头像上传接口返回） */
    private String avatar;
    /** 学号 / 工号 */
    private String userNo;
    /** 1学生 2教师 3职工 4访客 5家长（修改后需管理员重新审核） */
    private Integer identityType;
    /** 院系 / 部门 */
    private String department;
    /** 班级 */
    private String className;
}
