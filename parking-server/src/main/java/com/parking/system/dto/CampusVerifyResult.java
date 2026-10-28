package com.parking.system.dto;

/**
 * 第三方校园管理系统身份核验结果。
 *
 * @param status     核验状态
 * @param department 校园系统返回的院系/部门（未返回时为 null）
 * @param className  校园系统返回的班级（未返回时为 null）
 */
public record CampusVerifyResult(Status status, String department, String className) {

    public enum Status {
        /** 核验通过：校园系统存在该学号/工号，且返回的身份类型与提交一致 */
        MATCHED,
        /** 校园系统明确返回查无此人 */
        NOT_FOUND,
        /** 未配置或调用失败，无法判断，转人工审核 */
        UNAVAILABLE
    }

    public boolean matched() {
        return status == Status.MATCHED;
    }

    public static CampusVerifyResult matched(String department, String className) {
        return new CampusVerifyResult(Status.MATCHED, department, className);
    }

    public static CampusVerifyResult notFound() {
        return new CampusVerifyResult(Status.NOT_FOUND, null, null);
    }

    public static CampusVerifyResult unavailable() {
        return new CampusVerifyResult(Status.UNAVAILABLE, null, null);
    }
}
