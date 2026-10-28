package com.parking.framework.aspect;

import cn.hutool.json.JSONUtil;
import com.parking.common.UserContext;
import com.parking.common.annotation.OperationLog;
import com.parking.common.annotation.OperationType;
import com.parking.system.domain.Admin;
import com.parking.system.domain.AdminOperationLog;
import com.parking.system.mapper.AdminMapper;
import com.parking.system.mapper.AdminOperationLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 操作日志采集切面：环绕 {@link OperationLog} 注解的方法。
 * <p>
 * 在 finally 块写入 t_admin_operation_log，独立于业务事务：
 * <ul>
 *   <li>记录 admin_id / username（取自 UserContext，登录后由 JwtInterceptor 设置）</li>
 *   <li>operation / menu_code / type 来自注解</li>
 *   <li>uri / ip 取自 RequestContextHolder</li>
 *   <li>params 由 pjp.getArgs() 转 JSON 截断 2000 字符</li>
 *   <li>response_status 取 proceed 后的 response.getStatus()（200 成功 / 异常时由全局处理器决定）</li>
 *   <li>cost_ms 由 proceed 前后时间差计算</li>
 * </ul>
 * 采集自身失败时仅打 warn 日志，绝不影响业务调用。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final AdminOperationLogMapper operationLogMapper;
    private final AdminMapper adminMapper;

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint pjp, OperationLog operationLog) throws Throwable {
        long start = System.currentTimeMillis();
        Integer responseStatus = null;
        Throwable thrown = null;
        Object result = null;
        try {
            result = pjp.proceed();
            return result;
        } catch (Throwable t) {
            thrown = t;
            throw t;
        } finally {
            long cost = System.currentTimeMillis() - start;
            try {
                // proceed 后再读 response 状态码
                responseStatus = currentResponseStatus();
                if (responseStatus == null) {
                    // 异常时 response 状态码可能由全局异常处理器决定，统一记 200 表示业务已返回，
                    // 500 表示异常未处理
                    responseStatus = thrown == null ? 200 : 500;
                }
                writeLog(pjp, operationLog, cost, responseStatus);
            } catch (Exception e) {
                log.warn("Write operation log failed: {}", e.getMessage());
            }
        }
    }

    private void writeLog(ProceedingJoinPoint pjp, OperationLog annotation, long costMs, int responseStatus) {
        Long adminId = UserContext.getUserId();
        // 仅当后台管理员调用时采集；非管理员调用（如 user 走 /api/user/**）不会进入此切面
        boolean isAdminCall = "admin".equals(UserContext.getUserType());
        String username = null;

        if (isAdminCall) {
            if (adminId != null) {
                try {
                    Admin admin = adminMapper.selectById(adminId);
                    if (admin != null) username = admin.getUsername();
                } catch (Exception ignore) {
                    // 取不到用户名只是日志字段为空，不影响主流程
                }
            }
        }

        HttpServletRequest req = currentRequest();
        String uri = req != null ? req.getRequestURI() : null;
        String ip = req != null ? resolveIp(req) : null;

        String params = safeArgsToJson(pjp);
        if (params != null && params.length() > 2000) {
            params = params.substring(0, 2000);
        }

        AdminOperationLog entity = new AdminOperationLog();
        entity.setAdminId(adminId);
        entity.setUsername(username);
        entity.setOperation(annotation.value());
        entity.setMenuCode(annotation.menuCode());
        entity.setType(annotation.type() == null ? OperationType.UPDATE.name() : annotation.type().name());
        entity.setUri(uri);
        entity.setParams(params);
        entity.setResponseStatus(responseStatus);
        entity.setCostMs((int) costMs);
        entity.setIp(ip);
        entity.setOperationTime(LocalDateTime.now());
        operationLogMapper.insert(entity);
    }

    /**
     * 将方法参数转 JSON；含 MultipartFile / HttpServletResponse 等不可序列化对象时降级为 toString。
     */
    private String safeArgsToJson(ProceedingJoinPoint pjp) {
        try {
            Object[] args = pjp.getArgs();
            if (args == null || args.length == 0) return null;
            // 过滤掉不可序列化的类型（如 HttpServletRequest/MultipartFile/HttpServletResponse）
            Object[] filtered = Arrays.stream(args)
                    .filter(o -> !(o instanceof HttpServletRequest)
                            && !(o instanceof HttpServletResponse)
                            && !(o instanceof org.springframework.web.multipart.MultipartFile))
                    .toArray();
            if (filtered.length == 0) return "[]";
            return JSONUtil.toJsonStr(filtered);
        } catch (Exception e) {
            return Arrays.toString(pjp.getArgs());
        }
    }

    /**
     * 取当前线程的 HttpServletRequest（可能为空，如异步线程）。
     */
    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    /**
     * 取 proceed 之后的 HTTP 响应状态码（Spring MVC 默认 200）。
     */
    private Integer currentResponseStatus() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return null;
        HttpServletResponse response = attrs.getResponse();
        if (response == null) return null;
        return response.getStatus();
    }

    /**
     * 解析客户端 IP：X-Forwarded-For 优先，否则 RemoteAddr。
     */
    private String resolveIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // 多级代理时取第一个
            int comma = ip.indexOf(',');
            if (comma > 0) ip = ip.substring(0, comma).trim();
            return ip;
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) return ip;
        return request.getRemoteAddr();
    }
}
