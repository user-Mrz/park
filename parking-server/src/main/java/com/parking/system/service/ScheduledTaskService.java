package com.parking.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.parking.system.domain.MonthlyPass;
import com.parking.system.domain.ParkingSpot;
import com.parking.system.domain.Reservation;
import com.parking.system.mapper.MonthlyPassMapper;
import com.parking.system.mapper.ParkingSpotMapper;
import com.parking.system.mapper.ReservationMapper;
import com.parking.system.service.impl.SpotCacheServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Scheduled tasks for the parking system.
 * Runs periodic cleanup of expired reservations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTaskService {

    private final ReservationMapper reservationMapper;
    private final ParkingSpotMapper parkingSpotMapper;
    private final TidalSpotService tidalSpotService;
    private final SpotCacheService spotCacheService;
    private final MonthlyPassMapper monthlyPassMapper;
    private final SpotRealtimeService spotRealtimeService;
    private final StringRedisTemplate redisTemplate;

    @Value("${parking.reservation.grace-period-minutes:15}")
    private int gracePeriodMinutes;

    /**
     * 每 60 秒同步一次潮汐车位批次状态：待开放 -> 开放中 -> 已结束，
     * 并把已结束批次中仍空闲的车位停用，避免继续被统计为「可用车位」。
     * 注意：用户端可见性与入场门禁均按 openTime/closeTime 实时计算，本任务只负责状态与数据清理。
     */
    public void syncTidalBatches() {
        try {
            int changed = tidalSpotService.syncBatchStatus();
            if (changed > 0) {
                log.info("Synced {} tidal batch status(es) at {}", changed, LocalDateTime.now());
            }
        } catch (Exception e) {
            log.error("Failed to sync tidal batches: {}", e.getMessage());
        }
    }

    /**
     * 每日 00:05 将已到期的「生效中」月卡批量流转为「已过期」。
     * 读路径仍保留按到期日过滤，任务延迟或失败不会导致过期月卡被误用。
     */
    public void expireMonthlyPasses() {
        try {
            int changed = monthlyPassMapper.update(null, new LambdaUpdateWrapper<MonthlyPass>()
                    .eq(MonthlyPass::getStatus, 1)
                    .lt(MonthlyPass::getExpireDate, LocalDate.now())
                    .set(MonthlyPass::getStatus, 2)
                    .set(MonthlyPass::getUpdatedAt, LocalDateTime.now()));
            if (changed > 0) {
                log.info("Expired {} monthly pass(es) at {}", changed, LocalDateTime.now());
            }
        } catch (Exception e) {
            log.error("Failed to expire monthly passes: {}", e.getMessage());
        }
    }

    /**
     * Every 60 seconds, scan for active reservations whose endTime + grace period has passed,
     * mark them as expired, and release the parking spot.
     * Each reservation is processed independently to prevent one failure from rolling back all.
     */
    public void expireReservations() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(gracePeriodMinutes);
        List<Reservation> expiredList = reservationMapper.selectList(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getStatus, 1)
                        .lt(Reservation::getEndTime, cutoff)
        );
        if (expiredList.isEmpty()) return;
        int count = 0;
        for (Reservation r : expiredList) {
            try {
                expireOne(r);
                count++;
            } catch (Exception e) {
                log.error("Failed to expire reservation {}: {}", r.getId(), e.getMessage());
            }
        }
        if (count > 0) {
            log.info("Expired {} reservation(s) at {}", count, LocalDateTime.now());
        }
    }

    private void expireOne(Reservation r) {
        r.setStatus(4); // expired
        reservationMapper.updateById(r);
        ParkingSpot spot = parkingSpotMapper.selectById(r.getSpotId());
        if (spot != null && spot.getStatus() == 2) {
            spot.setStatus(0); // back to vacant
            parkingSpotMapper.updateById(spot);
            // 同步释放车位到缓存
            spotCacheService.setStatus(spot.getId(), 0);
        }
        spotRealtimeService.notifyChanged();
    }

    /**
     * 对账任务：周期性比对 Redis Hash {@code parking:spot:status} 与 MySQL {@code t_parking_spot} 表，
     * 发现状态差异时以 MySQL 为准刷新 Redis；清理 Redis 中已不存在于 MySQL 的 spotId。
     *
     * <p>兜底机制：在异步回写失败、缓存被外部篡改、Redis 异常重启等场景下保证最终一致。</p>
     */
    public void reconcileSpotCache() {
        Map<Object, Object> cacheEntries;
        try {
            cacheEntries = redisTemplate.opsForHash().entries(SpotCacheServiceImpl.CACHE_KEY);
        } catch (Exception e) {
            log.debug("reconcileSpotCache: Redis unavailable, skip: {}", e.getMessage());
            return;
        }
        if (cacheEntries.isEmpty()) {
            // 缓存为空可能是刚启动未预热完成或 Redis 重启；触发一次预热
            try {
                spotCacheService.initCache();
                log.info("reconcileSpotCache: cache empty, re-initialized");
            } catch (Exception e) {
                log.warn("reconcileSpotCache: re-init failed: {}", e.getMessage());
            }
            return;
        }
        // 从 MySQL 拉取全量 id+status
        List<ParkingSpot> all = parkingSpotMapper.selectList(null);
        Map<Long, Integer> dbMap = all.stream().collect(Collectors.toMap(
                ParkingSpot::getId,
                s -> s.getStatus() == null ? 0 : s.getStatus(),
                (a, b) -> a));
        int driftFixed = 0;
        int removed = 0;
        // 1. 修正 MySQL 中存在但状态不一致的 spot
        for (Map.Entry<Object, Object> e : cacheEntries.entrySet()) {
            Long spotId;
            try {
                spotId = Long.parseLong(e.getKey().toString());
            } catch (NumberFormatException ex) {
                continue;
            }
            Integer dbStatus = dbMap.get(spotId);
            if (dbStatus == null) {
                // Redis 有但 MySQL 已删除：清理
                redisTemplate.opsForHash().delete(SpotCacheServiceImpl.CACHE_KEY, e.getKey());
                removed++;
                continue;
            }
            Integer cacheStatus;
            try {
                cacheStatus = Integer.parseInt(e.getValue().toString());
            } catch (NumberFormatException ex) {
                redisTemplate.opsForHash().put(SpotCacheServiceImpl.CACHE_KEY,
                        e.getKey(), String.valueOf(dbStatus));
                driftFixed++;
                continue;
            }
            if (!cacheStatus.equals(dbStatus)) {
                redisTemplate.opsForHash().put(SpotCacheServiceImpl.CACHE_KEY,
                        e.getKey(), String.valueOf(dbStatus));
                driftFixed++;
            }
        }
        // 2. 补齐 MySQL 有但 Redis 缺失的 spot
        Set<Long> cacheKeys = new HashSet<>();
        for (Object k : cacheEntries.keySet()) {
            try {
                cacheKeys.add(Long.parseLong(k.toString()));
            } catch (NumberFormatException ignore) {}
        }
        int added = 0;
        for (Map.Entry<Long, Integer> e : dbMap.entrySet()) {
            if (!cacheKeys.contains(e.getKey())) {
                redisTemplate.opsForHash().put(SpotCacheServiceImpl.CACHE_KEY,
                        String.valueOf(e.getKey()), String.valueOf(e.getValue()));
                added++;
            }
        }
        if (driftFixed > 0 || removed > 0 || added > 0) {
            log.warn("reconcileSpotCache: driftFixed={}, removed={}, added={}",
                    driftFixed, removed, added);
        }
    }
}
