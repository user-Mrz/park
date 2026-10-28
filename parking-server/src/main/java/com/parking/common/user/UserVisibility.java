package com.parking.common.user;

import com.parking.common.IdentityType;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Set;

/**
 * 当前用户的「可见范围」：角色允许的停车区域集合 + 用于车位放行判定的身份类型。
 * <p>
 * 用户端 RBAC 的最终落地形态：
 * <ul>
 *   <li>zones：角色可见的区域（t_user_role_perm）；为空表示未限定区域</li>
 *   <li>identity：车位 allowIdentities 放行判定用的身份（未认证按访客处理，与计费口径一致）</li>
 * </ul>
 */
public class UserVisibility {

    private final Set<String> zones;
    private final int identity;

    public UserVisibility(Set<String> zones, int identity) {
        this.zones = zones == null ? Collections.emptySet() : zones;
        this.identity = identity;
    }

    public Set<String> getZones() {
        return zones;
    }

    public int getIdentity() {
        return identity;
    }

    /** 该区域 code 是否在可见区域内（zones 为空表示未限定） */
    public boolean zoneVisible(String zoneCode) {
        return zones.isEmpty() || zones.contains(zoneCode);
    }

    /** 该车位/批次（区域 + 允许身份）当前用户是否可见 */
    public boolean canSee(String zoneCode, String allowIdentities) {
        return zoneVisible(zoneCode) && identityAllowed(allowIdentities, identity);
    }

    /** 给定的「允许停放身份串」是否包含该身份（空串/空对象表示不限身份） */
    public static boolean identityAllowed(String allowIdentities, Integer identityType) {
        if (!StringUtils.hasText(allowIdentities)) return true;
        int identity = identityType == null ? IdentityType.VISITOR : identityType;
        for (String s : allowIdentities.split(",")) {
            if (s.trim().equals(String.valueOf(identity))) return true;
        }
        return false;
    }
}
