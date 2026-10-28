package com.parking.system.service;

import java.util.List;
import java.util.Map;

/**
 * 车位状态缓存服务：在 Redis 中缓存全量车位状态，高峰期承载预约/入场的 CAS 写入。
 *
 * <p>缓存结构：Redis Hash，key = {@code parking:spot:status}，field = spotId，value = 状态码（0/1/2/3）。
 * 状态语义与 {@link com.parking.system.domain.ParkingSpot#getStatus()} 一致：
 * 0 空闲 / 1 占用 / 2 已预约 / 3 维护中。</p>
 *
 * <p>覆盖范围：固定车位（spot_type=0）与潮汐车位（spot_type=1）共用同一张缓存，
 * 因为两者都在 {@code t_parking_spot} 表中，仅通过 spotType 区分业务规则。</p>
 *
 * <p>一致性模型：高峰期 Redis 先写 + {@code @Async} 异步回写 MySQL；
 * 非高峰期由调用方走原 MySQL CAS 流程。Redis 故障/缓存丢失时穿透回 MySQL 并自动重建。</p>
 */
public interface SpotCacheService {

    /** 启动预热：从 MySQL 把全部车位状态载入 Redis Hash（幂等，仅在 Hash 不存在时载入） */
    void initCache();

    /** 重新加载单个车位状态到缓存（缓存失效或管理端修改后调用） */
    void refreshSpot(Long spotId);

    /** 从缓存删除车位（车位被删除时调用） */
    void evict(Long spotId);

    /** 取车位状态：先查 Redis，未命中查 MySQL 并回填；车位不存在返回 null */
    Integer getStatus(Long spotId);

    /** 批量取状态：返回 Map，未命中的车位自动回填；不存在的车位不在结果中 */
    Map<Long, Integer> getStatuses(List<Long> spotIds);

    /** 高峰 CAS：0 -> 2（预约）。Redis 不可达或状态非空闲时返回 false */
    boolean tryReserve(Long spotId);

    /** 高峰 CAS：0|2 -> 1（入场）。Redis 不可达或状态非可入场时返回 false */
    boolean tryEnter(Long spotId);

    /** 释放车位（取消预约/出场）：无条件 set 0 */
    void release(Long spotId);

    /** 直接覆盖状态（管理端/对账任务用，不经过 CAS） */
    void setStatus(Long spotId, int status);

    /** 当前是否处于高峰时段：依据 SystemConfig {@code cache_peak_hours} 判定，空配置视为全程高峰 */
    boolean isPeakNow();

    /** 异步回写 MySQL 单车位状态（@Async，调用方不阻塞） */
    void asyncSyncToDb(Long spotId, int status);
}
