package com.parking.miniapp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parking.common.BusinessException;
import com.parking.common.UserContext;
import com.parking.common.user.UserVisibility;
import com.parking.system.dto.SpotOverviewDTO;
import com.parking.system.domain.*;
import com.parking.system.mapper.*;
import com.parking.miniapp.service.SpotService;
import com.parking.system.service.UserRoleService;
import com.parking.system.service.CampusRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SpotServiceImpl implements SpotService {

    private final ParkingSpotMapper parkingSpotMapper;
    private final VehicleRecordMapper vehicleRecordMapper;
    private final ReservationMapper reservationMapper;
    private final UserMapper userMapper;
    private final UserRoleService userRoleService;
    private final CampusRuleService campusRuleService;

    /**
     * 车位总览（areaGroups）的内存短缓存：所有用户共享同一份公共数据。
     * <p>避免高并发下每个请求都全表扫描 t_parking_spot。3 秒 TTL 足够及时，
     * 因为小程序端本身每 5 秒刷新一次，3 秒延迟完全在用户可接受范围。
     * parkedVehicles 与 activeReservations 是个性化数据，仍每请求查。</p>
     */
    private volatile List<SpotOverviewDTO.AreaGroup> areaGroupsCache;
    private volatile long areaGroupsCacheExpireAt = 0L;
    /** 缓存 TTL（毫秒）：3 秒，平衡数据新鲜度与数据库压力 */
    private static final long AREA_GROUPS_TTL_MS = 3000L;

    @Override
    public Page<ParkingSpot> getAvailableSpots(int page, int pageSize, String area) {
        LambdaQueryWrapper<ParkingSpot> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(area)) {
            wrapper.eq(ParkingSpot::getArea, area);
        }
        wrapper.ne(ParkingSpot::getStatus, 3); // exclude maintenance
        // 潮汐车位只在开放时间段内出现：开放前仅可预约（走 /spot/tidal），结束后不再展示
        filterTidalOpenNow(wrapper);
        // 用户端 RBAC：仅返回当前用户角色可见区域内、且允许其身份停放的车位；
        // 白名单用户豁免区域与身份限制，可见全部可用车位
        if (!isWhitelistedUser(UserContext.getUserId())) {
            applyVisibility(wrapper, userRoleService.visibilityOf(UserContext.getUserId()));
        }
        wrapper.orderByAsc(ParkingSpot::getSpotCode);
        return parkingSpotMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    /** 当前账号或其绑定车牌是否命中有效白名单 */
    private boolean isWhitelistedUser(Long userId) {
        if (userId == null) return false;
        User user = userMapper.selectById(userId);
        return user != null && campusRuleService.isWhitelisted(user.getPlateNumber(), userId);
    }

    /** 按用户可见范围追加过滤条件：可见区域 + 允许身份（在 SQL 侧过滤以保持分页计数正确） */
    private void applyVisibility(LambdaQueryWrapper<ParkingSpot> wrapper, UserVisibility vis) {
        if (!vis.getZones().isEmpty()) {
            wrapper.in(ParkingSpot::getZoneCode, vis.getZones());
        }
        wrapper.and(w -> w.isNull(ParkingSpot::getAllowIdentities)
                .or().eq(ParkingSpot::getAllowIdentities, "")
                .or().apply("FIND_IN_SET({0}, allow_identities) > 0", vis.getIdentity()));
    }

    /**
     * 固定车位始终可见；潮汐车位仅在其 [openTime, closeTime] 区间内可见。
     * 注意：这里按时间实时判断，不依赖定时任务写入的 status，避免调度延迟造成提前/延后放行。
     */
    private void filterTidalOpenNow(LambdaQueryWrapper<ParkingSpot> wrapper) {
        LocalDateTime now = LocalDateTime.now();
        wrapper.and(w -> w
                .eq(ParkingSpot::getSpotType, 0)
                .or(x -> x.eq(ParkingSpot::getSpotType, 1)
                        .le(ParkingSpot::getOpenTime, now)
                        .ge(ParkingSpot::getCloseTime, now)));
    }

    @Override
    public SpotOverviewDTO getOverview() {
        SpotOverviewDTO overview = new SpotOverviewDTO();

        // 越权防护：本接口只返回"当前登录用户自己"的停车数据。
        // 未登录时拦截器已返回 401，这里再做一次兜底，避免无上下文时把全量数据查出来。
        Long currentUserId = UserContext.getUserId();
        if (currentUserId == null) {
            throw new BusinessException("请先登录后再查看停车信息");
        }

        // 1. 车位总览走 3 秒短缓存（全量公共数据），再按当前用户角色的可见范围过滤；
        //    白名单用户豁免区域与身份限制，直接展示全部车位
        if (isWhitelistedUser(currentUserId)) {
            overview.setAreaGroups(getAreaGroupsCached());
        } else {
            UserVisibility visibility = userRoleService.visibilityOf(currentUserId);
            overview.setAreaGroups(filterByVisibility(getAreaGroupsCached(), visibility));
        }

        // 2. 只在场记录中查询"当前用户本人"的车辆（不再返回其他用户的车辆/车牌/手机号）
        List<VehicleRecord> parkedRecords = vehicleRecordMapper.selectList(
                new LambdaQueryWrapper<VehicleRecord>()
                        .eq(VehicleRecord::getUserId, currentUserId)
                        .eq(VehicleRecord::getStatus, 0)
                        .orderByDesc(VehicleRecord::getEntryTime));

        // 批量查用户和车位
        Set<Long> userIds = parkedRecords.stream().map(VehicleRecord::getUserId).collect(Collectors.toSet());
        Set<Long> spotIds = parkedRecords.stream().map(VehicleRecord::getSpotId).collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, ParkingSpot> spotMap = spotIds.isEmpty() ? Collections.emptyMap()
                : parkingSpotMapper.selectBatchIds(spotIds).stream().collect(Collectors.toMap(ParkingSpot::getId, Function.identity()));

        List<SpotOverviewDTO.ParkedVehicle> parkedVehicles = parkedRecords.stream().map(r -> {
            SpotOverviewDTO.ParkedVehicle pv = new SpotOverviewDTO.ParkedVehicle();
            pv.setRecordId(r.getId());
            pv.setSpotId(r.getSpotId());
            ParkingSpot spot = spotMap.get(r.getSpotId());
            pv.setSpotCode(spot != null ? spot.getSpotCode() : "#" + r.getSpotId());
            pv.setSpotPricePerHour(spot != null ? spot.getPricePerHour() : null);
            pv.setPlateNumber(r.getPlateNumber());
            pv.setEntryTime(r.getEntryTime());
            User user = userMap.get(r.getUserId());
            pv.setNickname(user != null ? user.getNickname() : "用户" + r.getUserId());
            pv.setPhone(user != null ? user.getPhone() : "");
            return pv;
        }).collect(Collectors.toList());
        overview.setParkedVehicles(parkedVehicles);

        // 3. 只返回"当前用户本人"的活跃预约（不再返回其他用户的预约、车牌、手机号）
        List<Reservation> activeReservations = reservationMapper.selectList(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getUserId, currentUserId)
                        .eq(Reservation::getStatus, 1)
                        .orderByDesc(Reservation::getCreatedAt));

        Set<Long> resUserIds = activeReservations.stream().map(Reservation::getUserId).collect(Collectors.toSet());
        Map<Long, User> resUserMap = resUserIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(resUserIds).stream().collect(Collectors.toMap(User::getId, Function.identity()));

        List<SpotOverviewDTO.ActiveReservation> activeResList = activeReservations.stream().map(r -> {
            SpotOverviewDTO.ActiveReservation ar = new SpotOverviewDTO.ActiveReservation();
            ar.setId(r.getId());
            ar.setUserId(r.getUserId());
            ar.setSpotId(r.getSpotId());
            ar.setReservationNo(r.getReservationNo());
            ar.setStartTime(r.getStartTime());
            ar.setEndTime(r.getEndTime());
            ar.setDurationHours(r.getDurationHours());
            ar.setTotalAmount(r.getTotalAmount());
            ar.setStatus(r.getStatus());
            User user = resUserMap.get(r.getUserId());
            ar.setNickname(user != null ? user.getNickname() : "用户" + r.getUserId());
            ar.setPhone(user != null ? user.getPhone() : "");
            ar.setPlateNumber(user != null ? user.getPlateNumber() : "");
            return ar;
        }).collect(Collectors.toList());
        overview.setActiveReservations(activeResList);

        return overview;
    }

    /**
     * 获取车位总览分组（带 3 秒内存短缓存）。
     * <p>所有用户共享同一份公共数据，无需每次全表扫描 t_parking_spot。
     * 缓存对象只读使用，多线程并发读安全；写入时由单线程构建后赋值给 volatile 引用。</p>
     * <p>失效条件：3 秒 TTL 到期；车位增删改通过管理端会触发下一周期自动重建。</p>
     */
    private List<SpotOverviewDTO.AreaGroup> getAreaGroupsCached() {
        long now = System.currentTimeMillis();
        List<SpotOverviewDTO.AreaGroup> cached = areaGroupsCache;
        if (cached != null && now < areaGroupsCacheExpireAt) {
            return cached;
        }
        // 缓存未命中或已过期：重建（多线程并发时可能重复构建，但结果一致，无需加锁）
        List<SpotOverviewDTO.AreaGroup> built = buildAreaGroups();
        areaGroupsCache = built;
        areaGroupsCacheExpireAt = now + AREA_GROUPS_TTL_MS;
        return built;
    }

    /**
     * 全量扫描 t_parking_spot 并按 area 分组（不含维护中车位；潮汐车位仅显示开放时段内）。
     */
    private List<SpotOverviewDTO.AreaGroup> buildAreaGroups() {
        LambdaQueryWrapper<ParkingSpot> spotWrapper = new LambdaQueryWrapper<>();
        spotWrapper.ne(ParkingSpot::getStatus, 3);  // exclude maintenance
        filterTidalOpenNow(spotWrapper);
        spotWrapper.orderByAsc(ParkingSpot::getSpotCode);
        List<ParkingSpot> allSpots = parkingSpotMapper.selectList(spotWrapper);
        Map<String, List<ParkingSpot>> grouped = allSpots.stream()
                .collect(Collectors.groupingBy(s -> s.getArea() != null ? s.getArea() : "未分区",
                        LinkedHashMap::new, Collectors.toList()));
        List<SpotOverviewDTO.AreaGroup> areaGroups = new ArrayList<>();
        grouped.keySet().stream().sorted().forEach(area -> {
            SpotOverviewDTO.AreaGroup ag = new SpotOverviewDTO.AreaGroup();
            ag.setArea(area);
            ag.setSpots(grouped.get(area).stream().map(s -> {
                SpotOverviewDTO.SpotInfo si = new SpotOverviewDTO.SpotInfo();
                si.setId(s.getId());
                si.setSpotCode(s.getSpotCode());
                si.setArea(s.getArea());
                si.setZoneCode(s.getZoneCode());
                si.setAllowIdentities(s.getAllowIdentities());
                si.setFloor(s.getFloor());
                si.setPricePerHour(s.getPricePerHour());
                si.setStatus(s.getStatus());
                return si;
            }).collect(Collectors.toList()));
            areaGroups.add(ag);
        });
        return areaGroups;
    }

    /**
     * 按用户可见范围过滤车位分组：保留角色可见区域内、且允许其身份停放的车位；
     * 过滤后为空的区域直接丢弃。结果全为新建对象，不改动缓存中的原始分组。
     */
    private List<SpotOverviewDTO.AreaGroup> filterByVisibility(
            List<SpotOverviewDTO.AreaGroup> source, UserVisibility vis) {
        List<SpotOverviewDTO.AreaGroup> result = new ArrayList<>();
        for (SpotOverviewDTO.AreaGroup g : source) {
            List<SpotOverviewDTO.SpotInfo> spots = g.getSpots().stream()
                    .filter(si -> vis.canSee(si.getZoneCode(), si.getAllowIdentities()))
                    .collect(Collectors.toList());
            if (spots.isEmpty()) continue;
            SpotOverviewDTO.AreaGroup ag = new SpotOverviewDTO.AreaGroup();
            ag.setArea(g.getArea());
            ag.setSpots(spots);
            result.add(ag);
        }
        return result;
    }
}
