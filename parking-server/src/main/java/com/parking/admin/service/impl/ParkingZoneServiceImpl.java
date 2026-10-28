package com.parking.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.parking.common.BusinessException;
import com.parking.system.dto.ParkingZoneDTO;
import com.parking.system.domain.ParkingSpot;
import com.parking.system.domain.ParkingZone;
import com.parking.system.mapper.ParkingSpotMapper;
import com.parking.system.mapper.ParkingZoneMapper;
import com.parking.admin.service.ParkingZoneService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 校园停车区域管理实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParkingZoneServiceImpl implements ParkingZoneService {

    private final ParkingZoneMapper parkingZoneMapper;
    private final ParkingSpotMapper parkingSpotMapper;

    /**
     * 系统内置区域：潮汐车位批次创建的车位固定归属 tidal 区域，
     * 一旦该区域被删除或停用，潮汐车位会在用户端整体消失，因此禁止这类操作。
     */
    private static final Set<String> BUILTIN_ZONE_CODES = Set.of("tidal");

    @Override
    public List<ParkingZone> listAll(Integer status) {
        List<ParkingZone> zones = parkingZoneMapper.selectList(new LambdaQueryWrapper<ParkingZone>()
                .eq(status != null, ParkingZone::getStatus, status)
                .orderByAsc(ParkingZone::getSortOrder)
                .orderByAsc(ParkingZone::getId));
        fillSpotCount(zones);
        zones.forEach(z -> z.setBuiltin(BUILTIN_ZONE_CODES.contains(z.getZoneCode())));
        return zones;
    }

    @Override
    public List<ParkingZone> listEnabled() {
        return parkingZoneMapper.selectList(new LambdaQueryWrapper<ParkingZone>()
                .eq(ParkingZone::getStatus, 1)
                .orderByAsc(ParkingZone::getSortOrder)
                .orderByAsc(ParkingZone::getId));
    }

    @Override
    @Transactional
    public ParkingZone create(ParkingZoneDTO dto) {
        String code = dto.getZoneCode().trim().toLowerCase();
        Long exists = parkingZoneMapper.selectCount(new LambdaQueryWrapper<ParkingZone>()
                .eq(ParkingZone::getZoneCode, code));
        if (exists != null && exists > 0) {
            throw new BusinessException("区域编码「" + code + "」已被占用，请换一个");
        }
        ParkingZone zone = new ParkingZone();
        zone.setZoneCode(code);
        zone.setZoneName(dto.getZoneName().trim());
        zone.setDescription(dto.getDescription() == null ? "" : dto.getDescription().trim());
        zone.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        zone.setStatus(1);
        parkingZoneMapper.insert(zone);
        zone.setSpotCount(0L);
        log.info("新增停车区域 code={} name={}", code, zone.getZoneName());
        return zone;
    }

    @Override
    @Transactional
    public ParkingZone update(Long id, ParkingZoneDTO dto) {
        ParkingZone zone = mustExist(id);
        if (!StringUtils.hasText(dto.getZoneName())) {
            throw new BusinessException("区域名称不能为空");
        }
        // 区域编码不参与修改：车位通过 zone_code 归属区域，改编码会导致既有车位变成孤儿
        zone.setZoneName(dto.getZoneName().trim());
        zone.setDescription(dto.getDescription() == null ? "" : dto.getDescription().trim());
        if (dto.getSortOrder() != null) zone.setSortOrder(dto.getSortOrder());
        parkingZoneMapper.updateById(zone);
        return zone;
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("区域状态取值仅支持 0（停用）或 1（启用）");
        }
        ParkingZone zone = mustExist(id);
        if (status == 0 && BUILTIN_ZONE_CODES.contains(zone.getZoneCode())) {
            throw new BusinessException("「" + zone.getZoneName() + "」是系统内置区域，不允许停用");
        }
        if (status == 0) {
            long spots = countSpots(zone.getZoneCode());
            if (spots > 0) {
                throw new BusinessException("该区域下仍有 " + spots + " 个车位，请先把车位调整到其他区域或删除后再停用");
            }
        }
        zone.setStatus(status);
        parkingZoneMapper.updateById(zone);
        log.info("停车区域 {} 状态改为 {}", zone.getZoneCode(), status == 1 ? "启用" : "停用");
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ParkingZone zone = mustExist(id);
        if (BUILTIN_ZONE_CODES.contains(zone.getZoneCode())) {
            throw new BusinessException("「" + zone.getZoneName() + "」是系统内置区域，不允许删除；"
                    + "删除后该区域下的车位会在用户端整体消失");
        }
        long spots = countSpots(zone.getZoneCode());
        if (spots > 0) {
            throw new BusinessException("该区域下仍有 " + spots + " 个车位，无法删除；请先调整或删除车位");
        }
        parkingZoneMapper.deleteById(id);
        log.info("删除停车区域 code={}", zone.getZoneCode());
    }

    /** 区域必须存在 */
    private ParkingZone mustExist(Long id) {
        if (id == null) throw new BusinessException("区域 ID 不能为空");
        ParkingZone zone = parkingZoneMapper.selectById(id);
        if (zone == null) throw new BusinessException("停车区域不存在");
        return zone;
    }

    private long countSpots(String zoneCode) {
        Long count = parkingSpotMapper.selectCount(new LambdaQueryWrapper<ParkingSpot>()
                .eq(ParkingSpot::getZoneCode, zoneCode));
        return count == null ? 0L : count;
    }

    /** 批量补齐各区域的车位数量，供管理端列表展示 */
    private void fillSpotCount(List<ParkingZone> zones) {
        if (zones == null || zones.isEmpty()) return;
        List<ParkingSpot> spots = parkingSpotMapper.selectList(new LambdaQueryWrapper<ParkingSpot>()
                .select(ParkingSpot::getZoneCode));
        Map<String, Long> countByCode = spots.stream()
                .filter(s -> StringUtils.hasText(s.getZoneCode()))
                .collect(Collectors.groupingBy(ParkingSpot::getZoneCode, Collectors.counting()));
        zones.forEach(z -> z.setSpotCount(countByCode.getOrDefault(z.getZoneCode(), 0L)));
    }
}
