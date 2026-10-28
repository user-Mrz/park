package com.parking.common.user;

/**
 * 小程序用户端 RBAC 相关常量。
 * <p>
 * 停车区域目录已迁移到「停车区域管理」（表 t_parking_zone），
 * 由 {@code ParkingZoneService} 统一维护，不再在此硬编码。
 */
public final class UserZoneMapping {

    private UserZoneMapping() {
    }

    /** 默认用户角色编码：未认证 / 微信新建用户（无校园身份）按访客处理 */
    public static final String DEFAULT_USER_ROLE = "visitor";
}
