package com.parking.system.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parking.common.BusinessException;
import com.parking.common.UserContext;
import com.parking.common.user.UserVisibility;
import com.parking.system.dto.TidalBatchDTO;
import com.parking.system.dto.TidalBatchVO;
import com.parking.system.domain.ParkingSpot;
import com.parking.system.domain.Reservation;
import com.parking.system.domain.TidalBatch;
import com.parking.system.domain.VehicleRecord;
import com.parking.system.mapper.ParkingSpotMapper;
import com.parking.system.mapper.ReservationMapper;
import com.parking.system.mapper.TidalBatchMapper;
import com.parking.system.mapper.VehicleRecordMapper;
import com.parking.system.service.CampusRuleService;
import com.parking.system.service.SpotCacheService;
import com.parking.system.service.SpotRealtimeService;
import com.parking.system.service.TidalSpotService;
import com.parking.system.service.UserRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 潮汐（临时）车位实现。
 *
 * <p>核心口径：<b>可见性与可入场判定全部按 openTime / closeTime 实时计算</b>，
 * 库中的 status 仅用于管理端展示与定时任务清理，不作为门禁依据。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TidalSpotServiceImpl implements TidalSpotService {

    /** 潮汐车位固定分区编码 */
    public static final String ZONE_TIDAL = "tidal";
    /** 默认提前预约天数 */
    private static final int DEFAULT_ADVANCE_DAYS = 7;
    private static final int MAX_SPOT_COUNT = 500;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final TidalBatchMapper tidalBatchMapper;
    private final ParkingSpotMapper parkingSpotMapper;
    private final ReservationMapper reservationMapper;
    private final VehicleRecordMapper vehicleRecordMapper;
    private final CampusRuleService campusRuleService;
    private final SpotCacheService spotCacheService;
    private final UserRoleService userRoleService;
    private final SpotRealtimeService spotRealtimeService;

    // ===================== 查询 =====================

    @Override
    public int advanceDays() {
        int days = campusRuleService.intConfig("tidal_reserve_advance_days", DEFAULT_ADVANCE_DAYS);
        if (days < 0) days = 0;
        if (days > 30) days = 30;
        return days;
    }

    @Override
    public Map<String, Object> listForUser() {
        LocalDateTime now = LocalDateTime.now();
        int advance = advanceDays();
        // 可见窗口：开放前 advance 天 ~ 结束时间；已取消/已删除的批次不展示
        List<TidalBatch> batches = tidalBatchMapper.selectList(new LambdaQueryWrapper<TidalBatch>()
                .notIn(TidalBatch::getStatus, TidalBatch.STATUS_CANCELLED, TidalBatch.STATUS_DELETED)
                .ge(TidalBatch::getCloseTime, now)
                .le(TidalBatch::getOpenTime, now.plusDays(advance))
                .orderByAsc(TidalBatch::getOpenTime));

        Map<Long, List<ParkingSpot>> spotMap = spotsGrouped(
                batches.stream().map(TidalBatch::getId).collect(Collectors.toList()));

        // 用户端 RBAC：仅展示当前用户角色可见区域内、且允许其身份停放的潮汐批次与车位
        UserVisibility vis = userRoleService.visibilityOf(UserContext.getUserId());
        List<TidalBatchVO> list = batches.stream()
                .filter(b -> vis.canSee(b.getZoneCode(), b.getAllowIdentities()))
                .map(b -> buildVO(b, filterSpots(spotMap.getOrDefault(b.getId(), Collections.emptyList()), vis), true))
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("advanceDays", advance);
        result.put("serverTime", LocalDateTime.now());
        result.put("batches", list);
        return result;
    }

    /** 按用户可见范围过滤批次内的车位（区域可见 + 允许身份） */
    private List<ParkingSpot> filterSpots(List<ParkingSpot> spots, UserVisibility vis) {
        if (spots == null || spots.isEmpty()) return Collections.emptyList();
        return spots.stream()
                .filter(s -> vis.canSee(s.getZoneCode(), s.getAllowIdentities()))
                .collect(Collectors.toList());
    }

    @Override
    public Page<TidalBatchVO> pageForAdmin(int page, int pageSize, Integer status, String keyword) {
        LambdaQueryWrapper<TidalBatch> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(TidalBatch::getStatus, status);
        else wrapper.ne(TidalBatch::getStatus, TidalBatch.STATUS_DELETED); // 软删除批次默认不在列表中展示
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(TidalBatch::getBatchName, kw).or().like(TidalBatch::getArea, kw));
        }
        wrapper.orderByDesc(TidalBatch::getOpenTime).orderByDesc(TidalBatch::getCreatedAt);
        Page<TidalBatch> p = tidalBatchMapper.selectPage(new Page<>(page, pageSize), wrapper);

        Map<Long, List<ParkingSpot>> spotMap = spotsGrouped(
                p.getRecords().stream().map(TidalBatch::getId).collect(Collectors.toList()));

        Page<TidalBatchVO> out = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        out.setRecords(p.getRecords().stream()
                .map(b -> buildVO(b, spotMap.getOrDefault(b.getId(), Collections.emptyList()), false))
                .collect(Collectors.toList()));
        return out;
    }

    @Override
    public TidalBatchVO detailForAdmin(Long id) {
        TidalBatch batch = tidalBatchMapper.selectById(id);
        if (batch == null) throw new BusinessException("潮汐车位批次不存在");
        return buildVO(batch, spotsOf(id), true);
    }

    @Override
    public TidalBatchVO toVO(TidalBatch batch, boolean withSpots) {
        if (batch == null) return null;
        return buildVO(batch, spotsOf(batch.getId()), withSpots);
    }

    private List<ParkingSpot> spotsOf(Long batchId) {
        return parkingSpotMapper.selectList(new LambdaQueryWrapper<ParkingSpot>()
                .eq(ParkingSpot::getTidalBatchId, batchId)
                .orderByAsc(ParkingSpot::getSpotCode));
    }

    private Map<Long, List<ParkingSpot>> spotsGrouped(List<Long> batchIds) {
        if (batchIds == null || batchIds.isEmpty()) return Collections.emptyMap();
        return parkingSpotMapper.selectList(new LambdaQueryWrapper<ParkingSpot>()
                        .in(ParkingSpot::getTidalBatchId, batchIds)
                        .orderByAsc(ParkingSpot::getSpotCode))
                .stream().collect(Collectors.groupingBy(ParkingSpot::getTidalBatchId));
    }

    /** 由批次实体与车位明细组装 VO（含 phase / canReserve / canEnter 等实时计算字段） */
    private TidalBatchVO buildVO(TidalBatch b, List<ParkingSpot> spots, boolean withSpots) {
        TidalBatchVO vo = new TidalBatchVO();
        vo.setId(b.getId());
        vo.setBatchName(b.getBatchName());
        vo.setArea(b.getArea());
        vo.setZoneCode(b.getZoneCode());
        vo.setSpotPrefix(b.getSpotPrefix());
        vo.setSpotCount(spots.isEmpty() ? (b.getSpotCount() == null ? 0 : b.getSpotCount()) : spots.size());
        vo.setFloor(b.getFloor());
        vo.setPricePerHour(b.getPricePerHour());
        vo.setAllowIdentities(b.getAllowIdentities());
        vo.setZoneIdentityNames(campusRuleService.identityNames(b.getAllowIdentities()));
        vo.setFreeMinutes(b.getFreeMinutes());
        vo.setOpenTime(b.getOpenTime());
        vo.setCloseTime(b.getCloseTime());
        vo.setRemark(b.getRemark());
        vo.setCreatedAt(b.getCreatedAt());
        vo.setStatus(b.getStatus());
        vo.setStatusLabel(statusLabel(b.getStatus()));

        int advance = advanceDays();
        vo.setAdvanceDays(advance);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime open = b.getOpenTime();
        LocalDateTime close = b.getCloseTime();
        boolean cancelled = b.getStatus() != null && b.getStatus() == TidalBatch.STATUS_CANCELLED;

        String phase;
        boolean canReserve;
        boolean canEnter;
        int daysUntilOpen = 0;
        LocalDateTime reserveFrom = null;

        if (open == null || close == null) {
            phase = "ended";
            canReserve = false;
            canEnter = false;
        } else {
            reserveFrom = open.minusDays(advance);
            if (now.isBefore(open)) {
                phase = "upcoming";
                canEnter = false;
                canReserve = !now.isBefore(reserveFrom);
                long minutes = Duration.between(now, open).toMinutes();
                daysUntilOpen = (int) Math.max(0, Math.ceil(minutes / 1440.0));
            } else if (!now.isAfter(close)) {
                phase = "open";
                canReserve = true;
                canEnter = true;
            } else {
                phase = "ended";
                canReserve = false;
                canEnter = false;
            }
        }
        if (cancelled) {
            canReserve = false;
            canEnter = false;
        }
        vo.setReserveFromTime(reserveFrom);
        vo.setPhase(phase);
        vo.setPhaseLabel(phaseLabel(phase));
        vo.setCanReserve(canReserve);
        vo.setCanEnter(canEnter);
        vo.setDaysUntilOpen(daysUntilOpen);

        int vacant = 0, occupied = 0, reserved = 0;
        for (ParkingSpot s : spots) {
            Integer st = s.getStatus() == null ? 0 : s.getStatus();
            if (st == 1) occupied++;
            else if (st == 2) reserved++;
            else if (st == 0) vacant++;
        }
        vo.setVacantCount(vacant);
        vo.setOccupiedCount(occupied);
        vo.setReservedCount(reserved);

        if (withSpots) {
            vo.setSpots(spots.stream().map(s -> {
                TidalBatchVO.SpotItem item = new TidalBatchVO.SpotItem();
                item.setId(s.getId());
                item.setSpotCode(s.getSpotCode());
                item.setStatus(s.getStatus());
                item.setFloor(s.getFloor());
                return item;
            }).collect(Collectors.toList()));
        }
        return vo;
    }

    private String phaseLabel(String phase) {
        switch (phase) {
            case "upcoming": return "开放前·仅可预约";
            case "open": return "开放中·可入场";
            default: return "已结束";
        }
    }

    private String statusLabel(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case TidalBatch.STATUS_PENDING: return "待开放";
            case TidalBatch.STATUS_OPEN: return "开放中";
            case TidalBatch.STATUS_CLOSED: return "已结束";
            case TidalBatch.STATUS_CANCELLED: return "已取消";
            case TidalBatch.STATUS_DELETED: return "已删除";
            default: return "未知";
        }
    }

    private String spotStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 1: return "有车在场";
            case 2: return "已被预约";
            case 3: return "维护中";
            default: return "空闲";
        }
    }

    // ===================== 管理端写操作 =====================

    @Override
    @Transactional
    public TidalBatchVO create(TidalBatchDTO dto) {
        validateForm(dto);
        String prefix = dto.getSpotPrefix().trim();
        int count = dto.getSpotCount();
        List<String> codes = buildCodes(prefix, count, 0);
        assertCodesFree(codes, null);

        TidalBatch batch = new TidalBatch();
        batch.setId(IdUtil.getSnowflake(1, 7).nextId());
        batch.setZoneCode(ZONE_TIDAL);
        applyForm(batch, dto);
        batch.setStatus(computeStatus(batch.getOpenTime(), batch.getCloseTime()));
        tidalBatchMapper.insert(batch);

        insertSpots(batch, codes);
        log.info("Created tidal batch {} ({} spots, {} ~ {})",
                batch.getBatchName(), codes.size(), batch.getOpenTime(), batch.getCloseTime());
        spotRealtimeService.notifyChanged();
        return toVO(tidalBatchMapper.selectById(batch.getId()), true);
    }

    @Override
    @Transactional
    public TidalBatchVO update(Long id, TidalBatchDTO dto) {
        TidalBatch batch = tidalBatchMapper.selectById(id);
        if (batch == null) throw new BusinessException("潮汐车位批次不存在");
        if (batch.getStatus() != null && batch.getStatus() == TidalBatch.STATUS_CANCELLED) {
            throw new BusinessException("该批次已取消，请先点击「恢复批次」后再修改");
        }
        validateForm(dto);

        List<ParkingSpot> spots = spotsOf(id);
        int current = spots.size();
        int target = dto.getSpotCount();
        List<String> finalCodes = buildCodes(dto.getSpotPrefix().trim(), target, 0);
        assertCodesFree(finalCodes, id);

        LocalDateTime oldClose = batch.getCloseTime();
        applyForm(batch, dto);
        int expectedStatus = computeStatus(batch.getOpenTime(), batch.getCloseTime());
        batch.setStatus(expectedStatus);
        tidalBatchMapper.updateById(batch);

        // 1) 缩小规模：从编号最大的车位开始删除，且只允许删除空闲车位
        if (target < current) {
            for (int i = target; i < current; i++) {
                ParkingSpot s = spots.get(i);
                if (s.getStatus() != null && s.getStatus() != 0) {
                    throw new BusinessException("车位 " + s.getSpotCode() + " 当前"
                            + spotStatusText(s.getStatus()) + "，无法减少车位数");
                }
            }
            for (int i = target; i < current; i++) {
                Long delId = spots.get(i).getId();
                parkingSpotMapper.deleteById(delId);
                // 车位被删除后从缓存移除
                spotCacheService.evict(delId);
            }
            spots = new ArrayList<>(spots.subList(0, target));
        }

        // 2) 扩大规模：追加新车位
        if (target > current) {
            List<String> addCodes = finalCodes.subList(current, target);
            insertSpots(batch, addCodes);
        }

        // 3) 同步已有车位的公共字段（含编号重命名；仅当批次此前已过期、现被延长到未来时才恢复车位）
        LocalDateTime now = LocalDateTime.now();
        boolean reopen = oldClose != null && oldClose.isBefore(now)
                && batch.getCloseTime() != null && batch.getCloseTime().isAfter(now);
        for (int i = 0; i < Math.min(current, target); i++) {
            ParkingSpot s = spots.get(i);
            s.setSpotCode(finalCodes.get(i));
            s.setArea(batch.getArea());
            s.setFloor(batch.getFloor());
            s.setPricePerHour(batch.getPricePerHour());
            s.setAllowIdentities(batch.getAllowIdentities());
            s.setFreeMinutes(batch.getFreeMinutes());
            s.setOpenTime(batch.getOpenTime());
            s.setCloseTime(batch.getCloseTime());
            s.setRemark("潮汐车位：" + batch.getBatchName());
            // 之前因批次结束被自动停用（status=3）的车位，在批次重新延长到未来后恢复为空闲
            if (reopen && s.getStatus() != null && s.getStatus() == 3) {
                s.setStatus(0);
            }
            parkingSpotMapper.updateById(s);
            // 字段或状态变更后刷新缓存（从 MySQL 取最新值回填）
            spotCacheService.refreshSpot(s.getId());
        }
        spotRealtimeService.notifyChanged();
        return toVO(tidalBatchMapper.selectById(id), true);
    }

    @Override
    @Transactional
    public void cancel(Long id) {
        TidalBatch batch = tidalBatchMapper.selectById(id);
        if (batch == null) throw new BusinessException("潮汐车位批次不存在");
        if (batch.getStatus() != null && batch.getStatus() == TidalBatch.STATUS_CANCELLED) {
            throw new BusinessException("该批次已取消");
        }
        List<ParkingSpot> spots = spotsOf(id);
        for (ParkingSpot s : spots) {
            if (s.getStatus() != null && (s.getStatus() == 1 || s.getStatus() == 2)) {
                throw new BusinessException("车位 " + s.getSpotCode() + " 当前"
                        + spotStatusText(s.getStatus()) + "，无法取消批次");
            }
        }
        if (!spots.isEmpty()) {
            List<Long> ids = spots.stream().map(ParkingSpot::getId).collect(Collectors.toList());
            Long active = reservationMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                    .in(Reservation::getSpotId, ids)
                    .eq(Reservation::getStatus, 1));
            if (active != null && active > 0) {
                throw new BusinessException("该批次仍有进行中的预约（" + active + " 条），请先处理后再取消");
            }
        }
        // 车位保留但置为「维护中」：既立刻对用户端不可见/不可入场，
        // 又保留车位行，使该批次的历史预约/停车记录仍可追溯（否则删除车位后就再也判断不出历史了）
        // 注意：不修改 openTime/closeTime，保留原始开放窗口，以便「恢复批次」能还原真实时段
        for (ParkingSpot s : spots) {
            if (s.getStatus() == null || s.getStatus() == 0) {
                s.setStatus(3);
                parkingSpotMapper.updateById(s);
                // 取消批次后置为维护中，同步缓存
                spotCacheService.setStatus(s.getId(), 3);
            }
        }
        batch.setStatus(TidalBatch.STATUS_CANCELLED);
        tidalBatchMapper.updateById(batch);
        log.info("Cancelled tidal batch {} and disabled {} spot(s)", id, spots.size());
        spotRealtimeService.notifyChanged();
    }

    @Override
    @Transactional
    public void remove(Long id) {
        TidalBatch batch = tidalBatchMapper.selectById(id);
        if (batch == null) throw new BusinessException("潮汐车位批次不存在");
        List<ParkingSpot> spots = spotsOf(id);
        for (ParkingSpot s : spots) {
            // 空闲(0)与已停用(3，批次取消或结束后置位)的车位允许随批次一并删除；在场/被预约的必须拒绝
            if (s.getStatus() != null && (s.getStatus() == 1 || s.getStatus() == 2)) {
                throw new BusinessException("车位 " + s.getSpotCode() + " 当前"
                        + spotStatusText(s.getStatus()) + "，无法删除批次");
            }
        }
        if (!spots.isEmpty()) {
            List<Long> ids = spots.stream().map(ParkingSpot::getId).collect(Collectors.toList());
            Long resCount = reservationMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                    .in(Reservation::getSpotId, ids));
            if (resCount != null && resCount > 0) {
                throw new BusinessException("该批次已产生预约记录，不能删除；如需停止使用请选择「取消批次」");
            }
            Long vehCount = vehicleRecordMapper.selectCount(new LambdaQueryWrapper<VehicleRecord>()
                    .in(VehicleRecord::getSpotId, ids));
            if (vehCount != null && vehCount > 0) {
                throw new BusinessException("该批次已产生停车记录，不能删除；如需停止使用请选择「取消批次」");
            }
        }
        for (ParkingSpot s : spots) {
            parkingSpotMapper.deleteById(s.getId());
            // 删除车位后从缓存移除
            spotCacheService.evict(s.getId());
        }
        tidalBatchMapper.deleteById(id);
        log.info("Deleted tidal batch {} and {} spot(s)", id, spots.size());
        spotRealtimeService.notifyChanged();
    }

    @Override
    @Transactional
    public void restore(Long id) {
        TidalBatch batch = tidalBatchMapper.selectById(id);
        if (batch == null) throw new BusinessException("潮汐车位批次不存在");
        if (batch.getStatus() == null || batch.getStatus() != TidalBatch.STATUS_CANCELLED) {
            throw new BusinessException("仅「已取消」的批次可以恢复");
        }
        int expected = computeStatus(batch.getOpenTime(), batch.getCloseTime());
        batch.setStatus(expected);
        batch.setUpdatedAt(LocalDateTime.now());
        tidalBatchMapper.updateById(batch);

        // 批次重新落入可开放阶段（待开放/开放中）时，把此前取消置为「维护中」的车位重新启用
        List<ParkingSpot> spots = spotsOf(id);
        if (expected != TidalBatch.STATUS_CLOSED) {
            for (ParkingSpot s : spots) {
                if (s.getStatus() != null && s.getStatus() == 3) {
                    s.setStatus(0);
                    parkingSpotMapper.updateById(s);
                    spotCacheService.setStatus(s.getId(), 0);
                }
            }
        }
        log.info("Restored tidal batch {} to status {}", id, expected);
        spotRealtimeService.notifyChanged();
    }

    @Override
    @Transactional
    public void softDelete(Long id) {
        TidalBatch batch = tidalBatchMapper.selectById(id);
        if (batch == null) throw new BusinessException("潮汐车位批次不存在");
        if (batch.getStatus() != null && batch.getStatus() == TidalBatch.STATUS_DELETED) {
            throw new BusinessException("该批次已删除");
        }
        // 车位与历史记录全部保留：仅标记批次为「已删除」并隐藏，
        // 仍空闲的车位置为维护中，避免继续对用户端可见/可预约
        List<ParkingSpot> spots = spotsOf(id);
        for (ParkingSpot s : spots) {
            if (s.getStatus() == null || s.getStatus() == 0) {
                s.setStatus(3);
                parkingSpotMapper.updateById(s);
                spotCacheService.setStatus(s.getId(), 3);
            }
        }
        batch.setStatus(TidalBatch.STATUS_DELETED);
        batch.setUpdatedAt(LocalDateTime.now());
        tidalBatchMapper.updateById(batch);
        log.info("Soft-deleted tidal batch {} (spots kept: {})", id, spots.size());
        spotRealtimeService.notifyChanged();
    }

    @Override
    @Transactional
    public void forceRemove(Long id) {
        TidalBatch batch = tidalBatchMapper.selectById(id);
        if (batch == null) throw new BusinessException("潮汐车位批次不存在");
        List<ParkingSpot> spots = spotsOf(id);
        // 仅拒绝仍未结束的实时占用：在场(1)/被预约(2)的车位不允许随批次一起物理删除
        for (ParkingSpot s : spots) {
            if (s.getStatus() != null && (s.getStatus() == 1 || s.getStatus() == 2)) {
                throw new BusinessException("车位 " + s.getSpotCode() + " 当前"
                        + spotStatusText(s.getStatus()) + "，无法彻底删除批次");
            }
        }
        for (ParkingSpot s : spots) {
            parkingSpotMapper.deleteById(s.getId());
            spotCacheService.evict(s.getId());
        }
        tidalBatchMapper.deleteById(id);
        log.warn("Force-deleted tidal batch {} and {} spot(s); related reservation/vehicle records keep dangling spotId", id, spots.size());
        spotRealtimeService.notifyChanged();
    }

    // ===================== 规则校验（被预约/入场调用） =====================

    @Override
    public void validateReservationWindow(ParkingSpot spot, LocalDateTime start, LocalDateTime end) {
        if (spot == null || !isTidal(spot)) return;
        LocalDateTime open = spot.getOpenTime();
        LocalDateTime close = spot.getCloseTime();
        if (open == null || close == null) {
            throw new BusinessException("该潮汐车位未配置开放时间，请联系停车管理处");
        }
        LocalDateTime now = LocalDateTime.now();
        int advance = advanceDays();
        LocalDateTime reserveFrom = open.minusDays(advance);
        if (now.isBefore(reserveFrom)) {
            throw new BusinessException("潮汐车位在开放时间前 " + advance + " 天才可预约，本批次 "
                    + fmt(open) + " 开放（" + fmt(reserveFrom) + " 起可预约）");
        }
        if (!now.isBefore(close)) {
            throw new BusinessException("该潮汐车位已于 " + fmt(close) + " 结束，无法预约");
        }
        if (!start.isBefore(open)) {
            // start >= open，合法
        } else {
            throw new BusinessException("预约开始时间不能早于潮汐车位开放时间 " + fmt(open));
        }
        if (end.isAfter(close)) {
            throw new BusinessException("预约结束时间不能晚于潮汐车位结束时间 " + fmt(close));
        }
    }

    @Override
    public void validateEnterWindow(ParkingSpot spot) {
        if (spot == null || !isTidal(spot)) return;
        LocalDateTime open = spot.getOpenTime();
        LocalDateTime close = spot.getCloseTime();
        if (open == null || close == null) {
            throw new BusinessException("该潮汐车位未配置开放时间，请联系停车管理处");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(open)) {
            throw new BusinessException("该潮汐车位将于 " + fmt(open) + " 开放，开放前仅可预约、暂不可入场");
        }
        if (now.isAfter(close)) {
            throw new BusinessException("该潮汐车位已于 " + fmt(close) + " 结束，不可入场");
        }
    }

    // ===================== 定时任务 =====================

    @Override
    @Transactional
    public int syncBatchStatus() {
        List<TidalBatch> list = tidalBatchMapper.selectList(new LambdaQueryWrapper<TidalBatch>()
                .notIn(TidalBatch::getStatus, TidalBatch.STATUS_CANCELLED, TidalBatch.STATUS_DELETED));
        int changed = 0;
        for (TidalBatch b : list) {
            try {
                int expected = computeStatus(b.getOpenTime(), b.getCloseTime());
                if (b.getStatus() == null || b.getStatus() != expected) {
                    b.setStatus(expected);
                    tidalBatchMapper.updateById(b);
                    changed++;
                }
                // 批次结束后，把仍空闲的车位置为维护中，避免继续被算作"可用车位"
                if (expected == TidalBatch.STATUS_CLOSED) {
                    // 先取出待变更车位 ID 列表，更新后同步缓存
                    List<ParkingSpot> closingSpots = parkingSpotMapper.selectList(
                            new LambdaQueryWrapper<ParkingSpot>()
                                    .eq(ParkingSpot::getTidalBatchId, b.getId())
                                    .eq(ParkingSpot::getStatus, 0));
                    parkingSpotMapper.update(null, new LambdaUpdateWrapper<ParkingSpot>()
                            .eq(ParkingSpot::getTidalBatchId, b.getId())
                            .eq(ParkingSpot::getStatus, 0)
                            .set(ParkingSpot::getStatus, 3));
                    for (ParkingSpot cs : closingSpots) {
                        spotCacheService.setStatus(cs.getId(), 3);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to sync tidal batch {}: {}", b.getId(), e.getMessage());
            }
        }
        spotRealtimeService.notifyChanged();
        return changed;
    }

    // ===================== 内部工具 =====================

    private boolean isTidal(ParkingSpot spot) {
        return spot.getSpotType() != null && spot.getSpotType() == 1;
    }

    private String fmt(LocalDateTime t) {
        return t == null ? "-" : t.format(FMT);
    }

    private int computeStatus(LocalDateTime open, LocalDateTime close) {
        LocalDateTime now = LocalDateTime.now();
        if (open == null || close == null) return TidalBatch.STATUS_CLOSED;
        if (now.isBefore(open)) return TidalBatch.STATUS_PENDING;
        if (now.isAfter(close)) return TidalBatch.STATUS_CLOSED;
        return TidalBatch.STATUS_OPEN;
    }

    private List<String> buildCodes(String prefix, int count, int fromIndex) {
        List<String> codes = new ArrayList<>(count);
        for (int i = fromIndex + 1; i <= count; i++) {
            codes.add(prefix + "-" + String.format("%03d", i));
        }
        return codes;
    }

    private void assertCodesFree(List<String> codes, Long excludeBatchId) {
        if (codes.isEmpty()) return;
        LambdaQueryWrapper<ParkingSpot> w = new LambdaQueryWrapper<ParkingSpot>()
                .in(ParkingSpot::getSpotCode, codes);
        if (excludeBatchId != null) {
            w.and(x -> x.isNull(ParkingSpot::getTidalBatchId).or().ne(ParkingSpot::getTidalBatchId, excludeBatchId));
        }
        Long dup = parkingSpotMapper.selectCount(w);
        if (dup != null && dup > 0) {
            throw new BusinessException("车位编号 " + codes.get(0) + " ~ " + codes.get(codes.size() - 1)
                    + " 与现有车位冲突，请更换编号前缀");
        }
    }

    private void insertSpots(TidalBatch batch, List<String> codes) {
        if (codes.isEmpty()) return;
        Snowflake sf = IdUtil.getSnowflake(1, 8);
        for (String code : codes) {
            ParkingSpot s = new ParkingSpot();
            s.setId(sf.nextId());
            s.setSpotCode(code);
            s.setArea(batch.getArea());
            s.setZoneCode(ZONE_TIDAL);
            s.setAllowIdentities(batch.getAllowIdentities());
            s.setFreeMinutes(batch.getFreeMinutes());
            s.setSpotType(1);
            s.setTidalBatchId(batch.getId());
            s.setOpenTime(batch.getOpenTime());
            s.setCloseTime(batch.getCloseTime());
            s.setFloor(batch.getFloor());
            s.setStatus(0);
            s.setPricePerHour(batch.getPricePerHour());
            s.setRemark("潮汐车位：" + batch.getBatchName());
            parkingSpotMapper.insert(s);
            // 新增车位同步进入缓存
            spotCacheService.setStatus(s.getId(), 0);
        }
    }

    private void applyForm(TidalBatch batch, TidalBatchDTO dto) {
        batch.setBatchName(dto.getBatchName().trim());
        batch.setArea(dto.getArea().trim());
        batch.setSpotPrefix(dto.getSpotPrefix().trim());
        batch.setSpotCount(dto.getSpotCount());
        batch.setFloor(dto.getFloor() == null ? "" : dto.getFloor().trim());
        batch.setPricePerHour(dto.getPricePerHour() == null ? BigDecimal.ZERO : dto.getPricePerHour());
        batch.setAllowIdentities(normalizeIdentities(dto.getAllowIdentities()));
        batch.setFreeMinutes(dto.getFreeMinutes() == null ? 0 : dto.getFreeMinutes());
        batch.setOpenTime(dto.getOpenTime());
        batch.setCloseTime(dto.getCloseTime());
        batch.setRemark(dto.getRemark() == null ? "" : dto.getRemark().trim());
    }

    private void validateForm(TidalBatchDTO dto) {
        if (dto == null) throw new BusinessException("请求参数不能为空");
        if (!StringUtils.hasText(dto.getBatchName())) throw new BusinessException("请填写批次名称");
        if (dto.getBatchName().trim().length() > 64) throw new BusinessException("批次名称不能超过 64 个字符");
        if (!StringUtils.hasText(dto.getArea())) throw new BusinessException("请填写显示区域名");
        if (dto.getArea().trim().length() > 64) throw new BusinessException("显示区域名不能超过 64 个字符");
        if (!StringUtils.hasText(dto.getSpotPrefix())) throw new BusinessException("请填写车位编号前缀");
        String prefix = dto.getSpotPrefix().trim();
        if (prefix.length() > 12) throw new BusinessException("车位编号前缀不能超过 12 个字符");
        if (!prefix.matches("[A-Za-z0-9\\u4e00-\\u9fa5]+")) {
            throw new BusinessException("车位编号前缀只能包含中文、字母或数字");
        }
        if (dto.getSpotCount() == null || dto.getSpotCount() < 1 || dto.getSpotCount() > MAX_SPOT_COUNT) {
            throw new BusinessException("车位数必须在 1 ~ " + MAX_SPOT_COUNT + " 之间");
        }
        if (dto.getOpenTime() == null || dto.getCloseTime() == null) {
            throw new BusinessException("请选择开放时间与结束时间");
        }
        if (!dto.getCloseTime().isAfter(dto.getOpenTime())) {
            throw new BusinessException("结束时间必须晚于开放时间");
        }
        long minutes = Duration.between(dto.getOpenTime(), dto.getCloseTime()).toMinutes();
        if (minutes < 30) throw new BusinessException("开放时间段不能短于 30 分钟");
        if (minutes > 60L * 24 * 180) throw new BusinessException("开放时间段不能超过 180 天");
        if (dto.getPricePerHour() == null || dto.getPricePerHour().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("每小时单价不能为负数");
        }
        if (dto.getPricePerHour().compareTo(new BigDecimal("9999")) > 0) {
            throw new BusinessException("每小时单价不能超过 9999 元");
        }
        Integer fm = dto.getFreeMinutes();
        if (fm != null && (fm < 0 || fm > 1440)) {
            throw new BusinessException("免费时长必须在 0 ~ 1440 分钟之间");
        }
        String allow = dto.getAllowIdentities();
        if (StringUtils.hasText(allow)) {
            for (String s : allow.split(",")) {
                String v = s.trim();
                if (v.isEmpty()) continue;
                int t;
                try {
                    t = Integer.parseInt(v);
                } catch (NumberFormatException e) {
                    throw new BusinessException("允许停放身份格式不正确：" + v);
                }
                if (t < 1 || t > 5) throw new BusinessException("允许停放身份取值必须为 1 ~ 5");
            }
        }
    }

    /** 身份串规范化：去重排序，空值统一为 null（表示不限身份） */
    private String normalizeIdentities(String allow) {
        if (!StringUtils.hasText(allow)) return null;
        TreeSet<Integer> set = new TreeSet<>();
        for (String s : allow.split(",")) {
            String v = s.trim();
            if (v.isEmpty()) continue;
            try {
                set.add(Integer.parseInt(v));
            } catch (NumberFormatException ignore) {
                // validateForm 已拦截
            }
        }
        if (set.isEmpty()) return null;
        return set.stream().map(String::valueOf).collect(Collectors.joining(","));
    }
}
