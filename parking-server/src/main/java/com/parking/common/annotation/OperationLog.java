package com.parking.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要采集操作日志的后台写操作方法。
 * <p>
 * 由 {@code OperationLogAspect} 用 @Around 环绕通知采集：
 * <ul>
 *   <li>operation：操作名（中文，给管理员看）</li>
 *   <li>menuCode：所属菜单 code，对应 {@link com.parking.common.menu.MenuPermissionMapping}</li>
 *   <li>type：CREATE / UPDATE / DELETE / AUDIT</li>
 * </ul>
 * 写入 t_admin_operation_log，失败时吞异常，不影响业务事务。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {
    /** 操作名（中文） */
    String value();
    /** 菜单 code */
    String menuCode();
    /** 操作类型 */
    OperationType type();
}
