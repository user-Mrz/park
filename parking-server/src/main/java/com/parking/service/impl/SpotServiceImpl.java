package com.parking.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parking.dto.SpotOverviewDTO;
import com.parking.entity.*;
import com.parking.mapper.*;
import com.parking.service.SpotService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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

    @Override
    public Page<ParkingSpot> getAvailableSpots(int page, int pageSize, String area) {
        LambdaQueryWrapper<ParkingSpot> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(area)) {
            wrapper.eq(ParkingSpot::getArea, area);
        }
        wrapper.ne(ParkingSpot::getStatus, 3); // exclude maintenance
        wrapper.orderByAsc(ParkingSpot::getSpotCode);
        return parkingSpotMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public SpotOverviewDTO getOverview() {
        SpotOverviewDTO overview = new SpotOverviewDTO();

        // 1. 所有车位按区域分组
        List<ParkingSpot> allSpots = parkingSpotMapper.selectList(
                new LambdaQueryWrapper<ParkingSpot>()
                        .ne(ParkingSpot::getStatus, 3)  // exclude maintenance
                        .orderByAsc(ParkingSpot::getSpotCode));
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
                si.setFloor(s.getFloor());
                si.setPricePerHour(s.getPricePerHour());
                si.setStatus(s.getStatus());
                return si;
            }).collect(Collectors.toList()));
            areaGroups.add(ag);
        });
        overview.setAreaGroups(areaGroups);

        // 2. 所有正在停车的记录（status = 0）
        List<VehicleRecord> parkedRecords = vehicleRecordMapper.selectList(
                new LambdaQueryWrapper<VehicleRecord>()
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

        // 3. 所有活跃预约（status = 1）
        List<Reservation> activeReservations = reservationMapper.selectList(
                new LambdaQueryWrapper<Reservation>()
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
}
