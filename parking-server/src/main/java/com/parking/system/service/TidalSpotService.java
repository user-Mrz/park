package com.parking.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parking.system.dto.TidalBatchDTO;
import com.parking.system.dto.TidalBatchVO;
import com.parking.system.domain.ParkingSpot;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 潮汐（临时）车位服务。
 *
 * <p>业务规则：管理员新建批次即批量生成带开放时间段的临时车位；用户端在开放时间前
 * {@code tidal_reserve_advance_days} 天（默认 7 天）起可以看到该批次，但此时只能预约、
 * 不能入场；进入 [openTime, closeTime] 后自动可入场；超过 closeTime 后不再对用户端展示。</p>
 */
public interface TidalSpotService {

    /** 用户端：可见的潮汐批次（含车位明细）。返回 {advanceDays, serverTime, batches} */
    Map<String, Object> listForUser();

    /** 管理端：分页查询批次 */
    Page<TidalBatchVO> pageForAdmin(int page, int pageSize, Integer status, String keyword);

    /** 管理端：批次详情（含车位明细） */
    TidalBatchVO detailForAdmin(Long id);

    /** 批量创建批次与车位 */
    TidalBatchVO create(TidalBatchDTO dto);

    /** 修改批次（时间窗、车位数、单价等），并同步到该批次全部车位 */
    TidalBatchVO update(Long id, TidalBatchDTO dto);

    /** 取消批次：仅在无在场车辆、无活跃预约时允许；取消后释放全部空闲车位 */
    void cancel(Long id);

    /** 删除批次：仅在该批次未产生任何预约/停车记录且车位全部空闲时允许 */
    void remove(Long id);

    /** 恢复批次：解除「已取消」锁定，按开放/结束时间重新计算状态并重新启用其车位（若批次重新落入可开放阶段） */
    void restore(Long id);

    /** 软删除批次：标记为「已删除」并从管理端列表隐藏，车位与预约/停车记录全部保留，便于溯源 */
    void softDelete(Long id);

    /** 彻底删除批次：物理删除批次与其全部车位（忽略预约/停车历史）；存在在场或被预约车位时拒绝 */
    void forceRemove(Long id);

    /** 提前预约天数（开放前 N 天可见并可预约） */
    int advanceDays();

    /** 预约前置校验：潮汐车位必须在可见期内，且预约时段落在开放时间段内 */
    void validateReservationWindow(ParkingSpot spot, LocalDateTime start, LocalDateTime end);

    /** 入场前置校验：潮汐车位必须处于开放时间段内 */
    void validateEnterWindow(ParkingSpot spot);

    /** 定时任务调用：同步已过期批次的库中状态，并停用其空闲车位 */
    int syncBatchStatus();

    /** 实体转 VO（withSpots=true 时附带车位明细） */
    TidalBatchVO toVO(com.parking.system.domain.TidalBatch batch, boolean withSpots);
}
