package com.parking.common.menu;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 菜单 code ↔ /api/admin/** 子路径前缀的静态映射工具类。
 * <p>
 * 用于 {@code MenuAuthInterceptor} 在请求到来时根据请求 URI 反查所属菜单 code，
 * 再判断当前管理员是否被分配了该菜单。
 * <p>
 * 设计原则：
 * 1. 一个菜单可对应多个子路径前缀（如 users 对应 /user 与 /users）
 * 2. 反查采用「最长前缀匹配」：如 /visit-reports/stats 同时命中 visits(/visit-report) 与 stats(/visit-reports/stats)，
 *    需保证更具体的路径优先；本表只列出菜单 code 级前缀，stats 仍归属 visits
 * 3. rules 与 config 共用 /config 前缀
 */
public final class MenuPermissionMapping {

    /** 内置超级管理员角色编码（t_role.role_code）：拥有全部权限，不受菜单分配限制 */
    public static final String SUPER_ADMIN_ROLE = "super_admin";

    /** 内置普通管理员角色编码：新建管理员账号的默认角色 */
    public static final String DEFAULT_ADMIN_ROLE = "admin";

    private MenuPermissionMapping() {}

    /**
     * 菜单 code → 子路径前缀列表（路径以 / 开头，匹配时只比较 requestUri 在 /api/admin 之后的部分）。
     */
    private static final Map<String, List<String>> MENU_TO_PREFIXES = new LinkedHashMap<>();

    /**
     * 超管专属菜单：即使分配给普通管理员也拒绝访问。
     */
    private static final Set<String> SUPER_ONLY = new LinkedHashSet<>();

    static {
        // 普通菜单
        MENU_TO_PREFIXES.put("dashboard",     Arrays.asList("/dashboard", "/campus-stats"));
        MENU_TO_PREFIXES.put("users",         Arrays.asList("/user", "/users", "/user-role", "/user-roles"));
        MENU_TO_PREFIXES.put("departments",   Arrays.asList("/department", "/departments"));
        MENU_TO_PREFIXES.put("spots",         Arrays.asList("/spot", "/spots", "/zone", "/zones"));
        MENU_TO_PREFIXES.put("tidal",          Arrays.asList("/tidal-batch", "/tidal-batches"));
        MENU_TO_PREFIXES.put("reservations",  Arrays.asList("/reservation", "/reservations"));
        MENU_TO_PREFIXES.put("vehicles",      Arrays.asList("/vehicle-record", "/vehicle-records"));
        MENU_TO_PREFIXES.put("visits",        Arrays.asList("/visit-report", "/visit-reports"));
        MENU_TO_PREFIXES.put("announcements", Arrays.asList("/announcement", "/announcements"));
        MENU_TO_PREFIXES.put("violations",    Arrays.asList("/violation", "/violations"));
        MENU_TO_PREFIXES.put("blacklist",     Arrays.asList("/blacklist", "/whitelist"));
        MENU_TO_PREFIXES.put("recharges",     Arrays.asList("/recharge-records"));
        MENU_TO_PREFIXES.put("payments",      Arrays.asList("/payment-records"));
        MENU_TO_PREFIXES.put("monthly_passes", Arrays.asList("/monthly-pass", "/monthly-passes"));
        MENU_TO_PREFIXES.put("config",        Arrays.asList("/config"));
        MENU_TO_PREFIXES.put("rules",         Arrays.asList("/config")); // 停车规则页共用 /config 接口
        MENU_TO_PREFIXES.put("server",        Arrays.asList("/server-monitor", "/spot-map"));
        MENU_TO_PREFIXES.put("jobs",          Arrays.asList("/job", "/jobs"));

        // 超管专属菜单
        MENU_TO_PREFIXES.put("accounts",        Arrays.asList("/account", "/accounts", "/role", "/roles"));
        MENU_TO_PREFIXES.put("login_logs",      Arrays.asList("/login-logs", "/login-log"));
        MENU_TO_PREFIXES.put("operation_logs",  Arrays.asList("/operation-logs", "/operation-log"));
        SUPER_ONLY.add("accounts");
        SUPER_ONLY.add("login_logs");
        SUPER_ONLY.add("operation_logs");
    }

    /**
     * 公共路径前缀：无论是否在 allowed 集合内都放行。
     * 注意：dashboard 已是普通菜单，可走 allowed 校验；这里只放超管专属接口本身需要的公共出口。
     */
    private static final Set<String> PUBLIC_PREFIXES = new LinkedHashSet<>(Arrays.asList(
            "/account/my-menus",  // 普通管理员登录后立即拉取自己的菜单 code 列表
            "/profile",           // 个人中心：任意角色管理员都能查看并修改自己的资料与密码
            "/spot-map"           // 车位地图是 dashboard 核心组件，所有管理员可见
    ));

    /**
     * 解析 /api/admin/** 请求 URI 所属的菜单 code。
     * <p>
     * 算法：取 /api/admin 之后相对路径，对每个菜单的前缀列表做最长匹配，命中则返回。
     * 如：
     *   /api/admin/users       -> users
     *   /api/admin/user/123    -> users
     *   /api/admin/visit-reports/stats -> visits
     *   /api/admin/account/123/menus -> accounts
     *
     * @param requestUri 完整请求 URI
     * @return menu_code；未匹配返回 null
     */
    public static String resolveMenuCode(String requestUri) {
        if (!StringUtils.hasText(requestUri)) return null;
        // 仅处理 /api/admin/** 范围
        final String prefix = "/api/admin";
        if (!requestUri.startsWith(prefix)) return null;
        String rel = requestUri.substring(prefix.length()); // 形如 /users 或 /user/123?...
        // 去掉查询串
        int q = rel.indexOf('?');
        if (q >= 0) rel = rel.substring(0, q);

        // 公共路径放行
        for (String p : PUBLIC_PREFIXES) {
            if (rel.equals(p) || rel.startsWith(p + "/") || rel.startsWith(p + "?")) {
                return null; // null 表示公共路径，由调用方直接放行
            }
        }

        // 数据导出：按导出类型归属对应业务菜单，
        // 防止低权限角色（如车位管理员）直接调用 /api/admin/export/users 导出全量用户资料。
        if (rel.startsWith("/export/")) {
            String type = rel.substring("/export/".length());
            switch (type) {
                case "reservations": return "reservations";
                case "payments":     return "payments";
                case "violations":   return "violations";
                case "visits":       return "visits";
                case "users":
                default:             return "users";
            }
        }

        // 最长前缀匹配：遍历所有菜单的所有前缀，选最长的
        String bestMenu = null;
        int bestLen = -1;
        for (Map.Entry<String, List<String>> e : MENU_TO_PREFIXES.entrySet()) {
            for (String p : e.getValue()) {
                if (rel.equals(p) || rel.startsWith(p + "/")) {
                    if (p.length() > bestLen) {
                        bestLen = p.length();
                        bestMenu = e.getKey();
                    }
                }
            }
        }
        return bestMenu;
    }

    /**
     * 判断指定菜单是否为超管专属（普通管理员即使被分配也拒绝）。
     */
    public static boolean isSuperOnly(String menuCode) {
        return menuCode != null && SUPER_ONLY.contains(menuCode);
    }

    /**
     * 返回全部菜单 code 列表（用于前端「分配菜单」对话框展示）。
     * 顺序：按定义顺序返回，超管专属在最后。
     */
    public static List<String> allMenuCodes() {
        return new ArrayList<>(MENU_TO_PREFIXES.keySet());
    }

    /**
     * 返回全部菜单 code 与描述的映射（前端用于显示菜单名）。
     */
    public static Map<String, String> allMenuLabels() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("dashboard",      "首页");
        m.put("users",          "用户管理");
        m.put("departments",    "学院管理");
        m.put("spots",          "常驻车位管理");
        m.put("tidal",          "潮汐车位管理");
        m.put("reservations",   "预约管理");
        m.put("vehicles",       "车辆进出");
        m.put("visits",         "访客与家长报备");
        m.put("announcements",  "公告管理");
        m.put("violations",     "违规记录");
        m.put("blacklist",      "黑白名单");
        m.put("recharges",      "充值记录");
        m.put("payments",       "扣费记录");
        m.put("monthly_passes", "月卡管理");
        m.put("config",         "系统配置");
        m.put("rules",          "停车规则");
        m.put("server",         "硬件监控");
        m.put("jobs",           "定时任务");
        m.put("accounts",       "管理员账号管理");
        m.put("login_logs",     "登录日志");
        m.put("operation_logs", "操作日志");
        return m;
    }

    /**
     * 返回超管专属菜单 code 列表。
     */
    public static Set<String> superOnlyMenus() {
        return Collections.unmodifiableSet(SUPER_ONLY);
    }
}
