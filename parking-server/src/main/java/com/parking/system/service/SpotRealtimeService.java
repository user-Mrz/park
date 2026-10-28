package com.parking.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.parking.common.user.UserVisibility;
import com.parking.framework.websocket.SpotSessionRegistry;
import com.parking.framework.websocket.SpotSessionRegistry.SpotSession;
import com.parking.system.domain.ParkingSpot;
import com.parking.system.mapper.ParkingSpotMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 车位状态实时推送服务。
 *
 * <p>业务侧只需调用 {@link #notifyChanged()} 置脏；本服务每秒读库比对并推送增量，
 * 每 30 秒推送一次全量快照兜底。这样既不需要在所有写车位状态的地方精确计算变化，
 * 也能避免事务尚未提交时推送错误数据。</p>
 *
 * <p>推送内容仅包含车位公开状态；在停车辆、预约、车主信息仍由现有 HTTP 接口提供。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpotRealtimeService {

    /** 登录过期关闭码（客户端据此停止重连并跳登录） */
    private static final int WS_CLOSE_TOKEN_EXPIRED = 4001;
    /** 心跳超时关闭码 */
    private static final int WS_CLOSE_HEARTBEAT_TIMEOUT = 4002;
    /** 超过该时长未收到 pong 视为连接失效 */
    private static final long HEARTBEAT_TIMEOUT_MS = 90_000L;
    /** 心跳间隔 */
    private static final long HEARTBEAT_INTERVAL_MS = 25_000L;

    private final ParkingSpotMapper parkingSpotMapper;
    private final SpotSessionRegistry sessionRegistry;

    /** 业务侧变更标记：高频调用也只是一次 CAS，开销可忽略 */
    private final AtomicBoolean dirty = new AtomicBoolean(false);
    /** 消息版本号：客户端据此丢弃乱序消息 */
    private final AtomicLong version = new AtomicLong(0L);
    /** 上一次发布的车位状态表 */
    private volatile Map<Long, SpotState> lastStates = Collections.emptyMap();

    /**
     * 业务 Service 在车位状态变更后调用（入场、出场、预约、管理员操作、潮汐批次等）。
     */
    public void notifyChanged() {
        dirty.set(true);
    }

    /**
     * 连接建立后补发一次当前快照。
     */
    public void sendSnapshotTo(SpotSession session) {
        Map<Long, SpotState> states = lastStates;
        if (states.isEmpty()) {
            states = refreshStates();
            lastStates = states;
        }
        sendSnapshot(session, states, version.get(), "connect");
    }

    /**
     * 每秒检查一次脏标记：有变更则读库、比对并广播 patch。
     */
    @Scheduled(fixedDelay = 1000, initialDelay = 1000)
    public void flushIfDirty() {
        if (!dirty.getAndSet(false)) {
            return;
        }
        try {
            Map<Long, SpotState> current = refreshStates();
            List<Change> changes = diff(lastStates, current);
            lastStates = current;
            if (changes.isEmpty()) {
                return;
            }
            long newVersion = version.incrementAndGet();
            broadcastPatch(changes, newVersion, counts(current));
        } catch (Exception e) {
            log.warn("车位状态增量推送失败：{}", e.getMessage());
        }
    }

    /**
     * 30 秒全量对账：修正漏事件、客户端漂移，以及潮汐车位进入/退出开放时段的可见性变化。
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 30000)
    public void reconcile() {
        try {
            Map<Long, SpotState> current = refreshStates();
            lastStates = current;
            if (sessionRegistry.size() == 0) {
                return;
            }
            long newVersion = version.incrementAndGet();
            for (SpotSession session : sessionRegistry.all()) {
                sendSnapshot(session, current, newVersion, "reconcile");
            }
        } catch (Exception e) {
            log.warn("车位状态定时对账失败：{}", e.getMessage());
        }
    }

    /**
     * 心跳与连接生命周期检查：token 过期或心跳超时的连接主动关闭。
     */
    @Scheduled(fixedRate = HEARTBEAT_INTERVAL_MS, initialDelay = HEARTBEAT_INTERVAL_MS)
    public void heartbeat() {
        long now = System.currentTimeMillis();
        for (SpotSession session : sessionRegistry.all()) {
            if (session.getTokenExpireAt() > 0 && now > session.getTokenExpireAt()) {
                sessionRegistry.sendErrorAndClose(session, WS_CLOSE_TOKEN_EXPIRED, "登录已过期，请重新登录");
                continue;
            }
            if (now - session.getLastPongAt() > HEARTBEAT_TIMEOUT_MS) {
                sessionRegistry.close(session, WS_CLOSE_HEARTBEAT_TIMEOUT, "心跳超时");
                continue;
            }
            Map<String, Object> ping = new LinkedHashMap<>();
            ping.put("type", "ping");
            ping.put("ts", now);
            sessionRegistry.send(session, ping);
        }
    }

    // ===================== 内部实现 =====================

    /** 从数据库读取车位状态表（只取推送需要的字段） */
    private Map<Long, SpotState> refreshStates() {
        List<ParkingSpot> spots = parkingSpotMapper.selectList(new LambdaQueryWrapper<ParkingSpot>()
                .select(ParkingSpot::getId, ParkingSpot::getSpotCode, ParkingSpot::getArea,
                        ParkingSpot::getZoneCode, ParkingSpot::getAllowIdentities, ParkingSpot::getStatus,
                        ParkingSpot::getSpotType, ParkingSpot::getOpenTime, ParkingSpot::getCloseTime));
        Map<Long, SpotState> states = new HashMap<>(Math.max(16, spots.size() * 2));
        for (ParkingSpot spot : spots) {
            if (spot.getId() == null) {
                continue;
            }
            states.put(spot.getId(), new SpotState(
                    spot.getId(),
                    spot.getSpotCode(),
                    spot.getArea(),
                    spot.getZoneCode(),
                    spot.getAllowIdentities(),
                    spot.getStatus() == null ? 0 : spot.getStatus(),
                    spot.getSpotType() == null ? 0 : spot.getSpotType(),
                    spot.getOpenTime(),
                    spot.getCloseTime()));
        }
        return states;
    }

    /** 只比较状态；新增/删除车位由 30 秒全量快照覆盖 */
    private List<Change> diff(Map<Long, SpotState> previous, Map<Long, SpotState> current) {
        List<Change> changes = new ArrayList<>();
        for (Map.Entry<Long, SpotState> entry : current.entrySet()) {
            SpotState old = previous.get(entry.getKey());
            if (old == null || old.status() != entry.getValue().status()) {
                changes.add(new Change(entry.getKey(), entry.getValue().status()));
            }
        }
        return changes;
    }

    private void broadcastPatch(List<Change> changes, long newVersion, Map<String, Object> adminCounts) {
        Map<String, Object> adminPayload = patchPayload(changes, newVersion, adminCounts);
        for (SpotSession session : sessionRegistry.all()) {
            if (session.isAdmin()) {
                sessionRegistry.send(session, adminPayload);
                continue;
            }
            List<Change> visibleChanges = new ArrayList<>();
            for (Change change : changes) {
                if (!session.getVisibleSpotIds().contains(change.id())) {
                    continue;
                }
                visibleChanges.add(change);
                if (change.status() == 3) {
                    // 维护中的车位对用户不可见，移出可见集合；恢复后由下一次快照带回
                    session.getVisibleSpotIds().remove(change.id());
                }
            }
            if (visibleChanges.isEmpty()) {
                continue;
            }
            sessionRegistry.send(session, patchPayload(visibleChanges, newVersion, null));
        }
    }

    private void sendSnapshot(SpotSession session, Map<Long, SpotState> states, long snapshotVersion, String reason) {
        LocalDateTime now = LocalDateTime.now();
        List<SpotState> ordered = new ArrayList<>(states.values());
        ordered.sort(Comparator.comparing(SpotState::spotCode, Comparator.nullsLast(String::compareTo)));

        List<Map<String, Object>> spots = new ArrayList<>();
        session.getVisibleSpotIds().clear();
        for (SpotState state : ordered) {
            if (!visibleTo(session, state, now)) {
                continue;
            }
            session.getVisibleSpotIds().add(state.id());
            spots.add(spotPayload(state));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "snapshot");
        payload.put("version", snapshotVersion);
        payload.put("ts", System.currentTimeMillis());
        payload.put("reason", reason);
        payload.put("spots", spots);
        if (session.isAdmin()) {
            payload.put("counts", counts(states));
        }
        sessionRegistry.send(session, payload);
    }

    /**
     * 用户端可见性：维护中不可见；潮汐车位仅开放时段可见；白名单豁免区域/身份；
     * 其余按 {@link UserVisibility} 过滤。管理员可见全部。
     */
    private boolean visibleTo(SpotSession session, SpotState state, LocalDateTime now) {
        if (session.isAdmin()) {
            return true;
        }
        if (state.status() == 3) {
            return false;
        }
        if (state.spotType() == 1) {
            if (state.openTime() == null || state.closeTime() == null) {
                return false;
            }
            if (now.isBefore(state.openTime()) || now.isAfter(state.closeTime())) {
                return false;
            }
        }
        if (session.isWhitelisted()) {
            return true;
        }
        UserVisibility visibility = session.getVisibility();
        return visibility == null || visibility.canSee(state.zoneCode(), state.allowIdentities());
    }

    private Map<String, Object> spotPayload(SpotState state) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", String.valueOf(state.id()));
        item.put("spotCode", state.spotCode());
        item.put("area", state.area());
        item.put("zoneCode", state.zoneCode());
        item.put("allowIdentities", state.allowIdentities());
        item.put("status", state.status());
        return item;
    }

    private Map<String, Object> patchPayload(List<Change> changes, long newVersion, Map<String, Object> counts) {
        List<Map<String, Object>> items = new ArrayList<>(changes.size());
        for (Change change : changes) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", String.valueOf(change.id()));
            item.put("status", change.status());
            items.add(item);
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "patch");
        payload.put("version", newVersion);
        payload.put("ts", System.currentTimeMillis());
        payload.put("changes", items);
        if (counts != null) {
            payload.put("counts", counts);
        }
        return payload;
    }

    /** 管理端汇总计数（与 getDashboard 的 availableSpots 口径一致：status=0） */
    private Map<String, Object> counts(Map<Long, SpotState> states) {
        int vacant = 0, occupied = 0, reserved = 0, maintenance = 0;
        for (SpotState state : states.values()) {
            switch (state.status()) {
                case 0 -> vacant++;
                case 1 -> occupied++;
                case 2 -> reserved++;
                case 3 -> maintenance++;
                default -> {
                    // 其他状态不计入汇总
                }
            }
        }
        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("vacant", vacant);
        counts.put("occupied", occupied);
        counts.put("reserved", reserved);
        counts.put("maintenance", maintenance);
        counts.put("total", states.size());
        return counts;
    }

    private record SpotState(Long id, String spotCode, String area, String zoneCode, String allowIdentities,
                             int status, int spotType, LocalDateTime openTime, LocalDateTime closeTime) {
    }

    private record Change(Long id, int status) {
    }
}
