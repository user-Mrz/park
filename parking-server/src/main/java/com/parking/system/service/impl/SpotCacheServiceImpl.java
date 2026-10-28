package com.parking.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.parking.system.domain.ParkingSpot;
import com.parking.system.mapper.ParkingSpotMapper;
import com.parking.system.service.CampusRuleService;
import com.parking.system.service.SpotCacheService;
import com.parking.system.service.SpotRealtimeService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 车位状态缓存服务实现（纯 Java 方案，无 Lua 脚本）。
 *
 * <p>核心实现要点：</p>
 * <ul>
 *   <li>使用 Redis Hash {@code parking:spot:status} 存储车位状态（field=spotId, value=状态码字符串）</li>
 *   <li>启动时通过 {@code @PostConstruct} 调用 {@link #initCache()} 从 MySQL 预热全量车位</li>
 *   <li>预约/入场的 CAS 用 {@link #lockFor(Long)} 分段锁 + HashOperations 实现「读-判-写」原子操作</li>
 *   <li>异步回写 MySQL 由 {@code @Async("parkingSyncExecutor")} 驱动，调用方不阻塞</li>
 *   <li>Redis 不可达时所有 CAS 返回 false / 状态查询返回 null，调用方需回退 MySQL 路径</li>
 * </ul>
 *
 * <p><b>并发模型说明</b>：分段锁方案在单 JVM 实例下完全保证原子性；
 * 若未来扩展为多实例部署，需升级为 Redis 分布式锁（Redisson）或恢复 Lua 脚本方案。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpotCacheServiceImpl implements SpotCacheService {

    /** Redis Hash key：全量车位状态缓存 */
    public static final String CACHE_KEY = "parking:spot:status";
    /** SystemConfig 键：高峰时段配置（运行时可覆盖 application.yml 默认值） */
    public static final String CONFIG_KEY_PEAK_HOURS = "cache_peak_hours";
    /** SystemConfig 键：缓存总开关（"false" 关闭缓存路径） */
    public static final String CONFIG_KEY_CACHE_ENABLED = "cache_enabled";

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    /** 分段锁数量：2^6=64 个锁足以分散车位级别的并发冲突，避免单锁串行化 */
    private static final int LOCK_STRIPES_COUNT = 64;
    private final Object[] lockStripes = initLockStripes();

    private Object[] initLockStripes() {
        Object[] arr = new Object[LOCK_STRIPES_COUNT];
        for (int i = 0; i < LOCK_STRIPES_COUNT; i++) {
            arr[i] = new Object();
        }
        return arr;
    }

    /** 根据 spotId 哈希到分段锁数组，相同 spotId 总是返回同一把锁 */
    private Object lockFor(Long spotId) {
        int idx = (spotId == null ? 0 : (spotId.hashCode() & 0x7FFFFFFF)) % LOCK_STRIPES_COUNT;
        return lockStripes[idx];
    }

    private final StringRedisTemplate redisTemplate;
    private final ParkingSpotMapper parkingSpotMapper;
    private final CampusRuleService campusRuleService;
    private final SpotRealtimeService spotRealtimeService;

    @Value("${parking.cache.peak-hours:}")
    private String defaultPeakHours;

    @Value("${parking.cache.enabled:true}")
    private boolean defaultCacheEnabled;

    @PostConstruct
    public void init() {
        try {
            initCache();
        } catch (Exception e) {
            // Redis 不可达不应阻塞应用启动；后续请求会穿透回 MySQL 并尝试重建缓存
            log.error("Init spot cache failed (Redis unavailable?): {}", e.getMessage());
        }
    }

    @Override
    public void initCache() {
        List<ParkingSpot> all = parkingSpotMapper.selectList(null);
        if (all.isEmpty()) {
            log.info("Spot cache init: no parking spots in DB, skip");
            return;
        }
        // 用 HMSET 一次性载入：构造 Map<spotId(String), status(String)>
        Map<String, String> bulk = new HashMap<>(all.size() * 2);
        for (ParkingSpot s : all) {
            int st = s.getStatus() == null ? 0 : s.getStatus();
            bulk.put(String.valueOf(s.getId()), String.valueOf(st));
        }
        redisTemplate.opsForHash().putAll(CACHE_KEY, bulk);
        log.info("Spot cache init: loaded {} spots into Redis Hash '{}'", all.size(), CACHE_KEY);
    }

    @Override
    public void refreshSpot(Long spotId) {
        if (spotId == null) return;
        try {
            ParkingSpot spot = parkingSpotMapper.selectById(spotId);
            if (spot == null) {
                redisTemplate.opsForHash().delete(CACHE_KEY, String.valueOf(spotId));
                return;
            }
            int st = spot.getStatus() == null ? 0 : spot.getStatus();
            redisTemplate.opsForHash().put(CACHE_KEY, String.valueOf(spotId), String.valueOf(st));
        } catch (Exception e) {
            log.warn("refreshSpot({}) failed: {}", spotId, e.getMessage());
        }
    }

    @Override
    public void evict(Long spotId) {
        if (spotId == null) return;
        try {
            redisTemplate.opsForHash().delete(CACHE_KEY, String.valueOf(spotId));
        } catch (Exception e) {
            log.warn("evict({}) failed: {}", spotId, e.getMessage());
        }
    }

    @Override
    public Integer getStatus(Long spotId) {
        if (spotId == null) return null;
        try {
            Object v = redisTemplate.opsForHash().get(CACHE_KEY, String.valueOf(spotId));
            if (v != null) {
                return Integer.parseInt(v.toString());
            }
            // 缓存未命中：穿透回 MySQL 并回填
            ParkingSpot spot = parkingSpotMapper.selectById(spotId);
            if (spot == null) return null;
            int st = spot.getStatus() == null ? 0 : spot.getStatus();
            try {
                redisTemplate.opsForHash().put(CACHE_KEY, String.valueOf(spotId), String.valueOf(st));
            } catch (Exception e) {
                log.debug("Cache backfill failed for spot {}: {}", spotId, e.getMessage());
            }
            return st;
        } catch (Exception e) {
            log.warn("getStatus({}) Redis failed, fallback to MySQL: {}", spotId, e.getMessage());
            ParkingSpot spot = parkingSpotMapper.selectById(spotId);
            return spot == null ? null : (spot.getStatus() == null ? 0 : spot.getStatus());
        }
    }

    @Override
    public Map<Long, Integer> getStatuses(List<Long> spotIds) {
        if (spotIds == null || spotIds.isEmpty()) return Collections.emptyMap();
        Map<Long, Integer> result = new HashMap<>(spotIds.size() * 2);
        List<Long> missed = new ArrayList<>();
        // 先批量取 Redis
        try {
            List<Object> fields = spotIds.stream().map(String::valueOf).collect(Collectors.toList());
            List<Object> values = redisTemplate.opsForHash().multiGet(CACHE_KEY, fields);
            for (int i = 0; i < spotIds.size(); i++) {
                Object v = values.get(i);
                if (v != null) {
                    result.put(spotIds.get(i), Integer.parseInt(v.toString()));
                } else {
                    missed.add(spotIds.get(i));
                }
            }
        } catch (Exception e) {
            log.warn("getStatuses Redis failed, fallback all to MySQL: {}", e.getMessage());
            missed.addAll(spotIds);
        }
        // 穿透回 MySQL 并回填缺失项
        if (!missed.isEmpty()) {
            List<ParkingSpot> spots = parkingSpotMapper.selectBatchIds(missed);
            Map<Long, Integer> dbMap = new HashMap<>(spots.size() * 2);
            for (ParkingSpot s : spots) {
                int st = s.getStatus() == null ? 0 : s.getStatus();
                dbMap.put(s.getId(), st);
            }
            // 回填缓存
            try {
                Map<String, String> backfill = new HashMap<>(dbMap.size() * 2);
                for (Map.Entry<Long, Integer> e : dbMap.entrySet()) {
                    backfill.put(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
                }
                if (!backfill.isEmpty()) {
                    redisTemplate.opsForHash().putAll(CACHE_KEY, backfill);
                }
            } catch (Exception e) {
                log.debug("Backfill failed for {} spots: {}", dbMap.size(), e.getMessage());
            }
            result.putAll(dbMap);
        }
        return result;
    }

    @Override
    public boolean tryReserve(Long spotId) {
        if (spotId == null) return false;
        String field = String.valueOf(spotId);
        synchronized (lockFor(spotId)) {
            try {
                Object v = redisTemplate.opsForHash().get(CACHE_KEY, field);
                if (v == null || !"0".equals(v.toString())) {
                    return false;
                }
                redisTemplate.opsForHash().put(CACHE_KEY, field, "2");
                return true;
            } catch (Exception e) {
                log.warn("tryReserve({}) Redis failed: {}", spotId, e.getMessage());
                return false;
            }
        }
    }

    @Override
    public boolean tryEnter(Long spotId) {
        if (spotId == null) return false;
        String field = String.valueOf(spotId);
        synchronized (lockFor(spotId)) {
            try {
                Object v = redisTemplate.opsForHash().get(CACHE_KEY, field);
                if (v == null) return false;
                String cur = v.toString();
                if (!"0".equals(cur) && !"2".equals(cur)) {
                    return false;
                }
                redisTemplate.opsForHash().put(CACHE_KEY, field, "1");
                return true;
            } catch (Exception e) {
                log.warn("tryEnter({}) Redis failed: {}", spotId, e.getMessage());
                return false;
            }
        }
    }

    @Override
    public void release(Long spotId) {
        if (spotId == null) return;
        String field = String.valueOf(spotId);
        synchronized (lockFor(spotId)) {
            try {
                redisTemplate.opsForHash().put(CACHE_KEY, field, "0");
            } catch (Exception e) {
                log.warn("release({}) Redis failed: {}", spotId, e.getMessage());
            }
        }
    }

    @Override
    public void setStatus(Long spotId, int status) {
        if (spotId == null) return;
        try {
            redisTemplate.opsForHash().put(CACHE_KEY, String.valueOf(spotId), String.valueOf(status));
        } catch (Exception e) {
            log.warn("setStatus({}, {}) Redis failed: {}", spotId, status, e.getMessage());
        }
    }

    @Override
    public boolean isPeakNow() {
        // 1. 总开关：SystemConfig 优先，application.yml 兜底
        String enabledCfg = campusRuleService.config(CONFIG_KEY_CACHE_ENABLED, null);
        boolean enabled = enabledCfg != null ? !"false".equalsIgnoreCase(enabledCfg.trim())
                : defaultCacheEnabled;
        if (!enabled) return false;

        // 2. 高峰时段：SystemConfig 优先，application.yml 兜底
        String peakHours = campusRuleService.config(CONFIG_KEY_PEAK_HOURS, defaultPeakHours);
        if (peakHours == null || peakHours.trim().isEmpty()) {
            // 未配置视为全程高峰（始终走 Redis 路径）
            return true;
        }
        List<TimeRange> ranges = parsePeakHours(peakHours);
        if (ranges.isEmpty()) {
            log.warn("Invalid peak-hours config '{}', treat as always-peak", peakHours);
            return true;
        }
        LocalTime now = LocalTime.now();
        for (TimeRange r : ranges) {
            if (!now.isBefore(r.start) && !now.isAfter(r.end)) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Async("parkingSyncExecutor")
    public void asyncSyncToDb(Long spotId, int status) {
        if (spotId == null) return;
        try {
            int rows = parkingSpotMapper.update(null,
                    new LambdaUpdateWrapper<ParkingSpot>()
                            .eq(ParkingSpot::getId, spotId)
                            .set(ParkingSpot::getStatus, status));
            if (rows == 0) {
                log.warn("asyncSyncToDb: spot {} not found in MySQL (rows=0), evict from cache", spotId);
                evict(spotId);
            } else {
                log.debug("asyncSyncToDb: spot {} -> status {} synced", spotId, status);
                spotRealtimeService.notifyChanged();
            }
        } catch (Exception e) {
            log.error("asyncSyncToDb failed for spot {} status {}: {}", spotId, status, e.getMessage());
            // 失败时由对账任务最终兜底
        }
    }

    /** 解析高峰时段配置：格式 "07:00-09:00,16:30-19:00"，多段逗号分隔 */
    private List<TimeRange> parsePeakHours(String cfg) {
        List<TimeRange> result = new ArrayList<>();
        for (String seg : cfg.split(",")) {
            String[] parts = seg.trim().split("-");
            if (parts.length != 2) continue;
            try {
                LocalTime start = LocalTime.parse(parts[0].trim(), HH_MM);
                LocalTime end = LocalTime.parse(parts[1].trim(), HH_MM);
                if (!end.isAfter(start)) {
                    // 跨午夜（如 23:00-02:00）暂不支持，记录并跳过
                    log.warn("Peak segment {} has end <= start, skip", seg);
                    continue;
                }
                result.add(new TimeRange(start, end));
            } catch (Exception e) {
                log.warn("Invalid peak segment '{}': {}", seg, e.getMessage());
            }
        }
        return result;
    }

    private record TimeRange(LocalTime start, LocalTime end) {}
}
