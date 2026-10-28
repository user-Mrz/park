package com.parking.common.annotation;

/**
 * 后台关键写操作类型枚举（用于操作日志 type 字段）。
 */
public enum OperationType {
    /** 新增 */
    CREATE,
    /** 修改 */
    UPDATE,
    /** 删除 */
    DELETE,
    /** 审核 */
    AUDIT
}
