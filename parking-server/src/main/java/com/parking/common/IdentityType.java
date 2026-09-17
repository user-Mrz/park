package com.parking.common;

/**
 * 校园身份类型与认证状态常量
 */
public final class IdentityType {

    private IdentityType() {
    }

    public static final int STUDENT = 1;
    public static final int TEACHER = 2;
    public static final int STAFF = 3;
    public static final int VISITOR = 4;
    public static final int PARENT = 5;

    /** 身份认证状态 */
    public static final int AUDIT_PENDING = 0;
    public static final int AUDIT_APPROVED = 1;
    public static final int AUDIT_REJECTED = 2;

    public static String label(Integer type) {
        if (type == null) return "未知";
        switch (type) {
            case STUDENT: return "学生";
            case TEACHER: return "教师";
            case STAFF: return "职工";
            case VISITOR: return "访客";
            case PARENT: return "家长";
            default: return "未知";
        }
    }

    public static String auditLabel(Integer status) {
        if (status == null) return "待审核";
        switch (status) {
            case AUDIT_APPROVED: return "已认证";
            case AUDIT_REJECTED: return "已驳回";
            default: return "待审核";
        }
    }
}
