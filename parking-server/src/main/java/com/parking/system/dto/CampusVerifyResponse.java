package com.parking.system.dto;

import lombok.Data;

/**
 * 第三方校园管理系统身份核验接口的响应结构。
 * <p>期望格式：{@code {"code":200,"data":{"matched":true,"userNo":"...","identityType":1}}}</p>
 */
@Data
public class CampusVerifyResponse {

    /** 业务状态码，200 表示接口调用成功 */
    private int code;

    private String message;

    private IdentityData data;

    @Data
    public static class IdentityData {
        /** 校园系统是否查询到该学号/工号 */
        private boolean matched;

        /** 校园系统返回的学号/工号，用于与提交值比对 */
        private String userNo;

        /** 校园系统返回的身份类型，用于防止冒用身份 */
        private Integer identityType;

        private String department;

        private String className;
    }
}
