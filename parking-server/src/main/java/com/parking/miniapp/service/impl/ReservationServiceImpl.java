package com.parking.miniapp.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.parking.common.BusinessException;
import com.parking.system.dto.ReservationDTO;
import com.parking.system.domain.ParkingSpot;
import com.parking.system.domain.Reservation;
import com.parking.system.domain.User;
import com.parking.system.mapper.ParkingSpotMapper;
import com.parking.system.mapper.ReservationMapper;
import com.parking.system.mapper.UserMapper;
import com.parking.system.service.CampusRuleService;
import com.parking.miniapp.service.ReservationService;
import com.parking.system.service.SpotCacheService;
import com.parking.system.service.SpotRealtimeService;
import com.parking.system.service.TidalSpotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private final ReservationMapper reservationMapper;
    private final ParkingSpotMapper parkingSpotMapper;
    private final CampusRuleService campusRuleService;
    private final TidalSpotService tidalSpotService;
    private final UserMapper userMapper;
    private final SpotCacheService spotCacheService;
    private final SpotRealtimeService spotRealtimeService;

    @Override
    @Transactional
    public Reservation createReservation(Long userId, ReservationDTO dto) {
        if (dto.getStartTime() == null || dto.getEndTime() == null) {
            throw new BusinessException("请选择预约的开始时间与结束时间");
        }
        LocalDateTime now = LocalDateTime.now();
        // 约束：不允许预约当前时间之前的时段（前端已过滤历史时段，此处为服务端兜底）
        if (dto.getStartTime().isBefore(now)) {
            throw new BusinessException("预约开始时间不能早于当前时间，请重新选择");
        }
        if (!dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new BusinessException("预约结束时间必须晚于开始时间");
        }
        long minutes = Duration.between(dto.getStartTime(), dto.getEndTime()).toMinutes();
        if (minutes < 60) throw new BusinessException("最短预约时长为 1 小时");
        long hours = minutes / 60;
        long days = hours / 24;
        if (days > 30) throw new BusinessException("最长预约时长为 30 天");
        ParkingSpot spot = parkingSpotMapper.selectById(dto.getSpotId());
        if (spot == null) throw new BusinessException("车位不存在");
        // 校园规则校验：身份认证 / 黑名单 / 区域权限 / 假期模式 / 夜间禁停 / 每日预约上限
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        campusRuleService.validateReservation(user, spot, dto.getStartTime(), dto.getEndTime());
        // 潮汐车位专属校验：必须在「开放前 N 天」的可见期内，且预约时段落在开放时间段内
        tidalSpotService.validateReservationWindow(spot, dto.getStartTime(), dto.getEndTime());
        // 状态校验 + CAS：高峰期先查 Redis 缓存 + Redis Lua CAS + 异步回写 MySQL
        // 非高峰走 MySQL 实体状态 + MySQL CAS（保持原逻辑）
        boolean peak = spotCacheService.isPeakNow();
        Integer currentStatus = peak
                ? spotCacheService.getStatus(dto.getSpotId()) : spot.getStatus();
        if (currentStatus == null || currentStatus != 0) {
            throw new BusinessException("该车位当前不可预约");
        }
        boolean casOk;
        if (peak) {
            casOk = spotCacheService.tryReserve(dto.getSpotId());
            if (casOk) {
                spotCacheService.asyncSyncToDb(dto.getSpotId(), 2);
                log.debug("Peak reservation: spot {} CAS ok via Redis, async sync to MySQL", dto.getSpotId());
            }
        } else {
            int rows = parkingSpotMapper.update(null,
                    new LambdaUpdateWrapper<ParkingSpot>()
                            .eq(ParkingSpot::getId, dto.getSpotId())
                            .eq(ParkingSpot::getStatus, 0)  // 只有空闲状态才能预约
                            .set(ParkingSpot::getStatus, 2));
            casOk = rows > 0;
            // 非高峰也同步更新缓存（若缓存已就绪）
            spotCacheService.setStatus(dto.getSpotId(), 2);
        }
        if (!casOk) throw new BusinessException("该车位刚被他人预约，请重新选择");
        BigDecimal durationHours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 1, RoundingMode.HALF_UP);
        // 校园身份费率：学生折扣 / 访客加价 / 假期折扣（免费时长在出场结算时抵扣）
        BigDecimal totalAmount = campusRuleService.calculateReservationAmount(user, spot, durationHours);
        Snowflake sf = IdUtil.getSnowflake(1, 3);
        Reservation reservation = new Reservation();
        reservation.setId(sf.nextId());
        reservation.setUserId(userId);
        reservation.setSpotId(dto.getSpotId());
        reservation.setReservationNo("RES" + sf.nextId());
        reservation.setStartTime(dto.getStartTime());
        reservation.setEndTime(dto.getEndTime());
        reservation.setDurationHours(durationHours);
        reservation.setTotalAmount(totalAmount);
        reservation.setStatus(1); // active
        reservationMapper.insert(reservation);
        spotRealtimeService.notifyChanged();
        return reservation;
    }

    @Override
    @Transactional
    public void cancelReservation(Long userId, Long reservationId) {
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) throw new BusinessException("预约记录不存在");
        if (!reservation.getUserId().equals(userId)) throw new BusinessException(403, "该预约不属于当前账号，无权操作");
        if (reservation.getStatus() != 1) {
            throw new BusinessException("只有进行中的预约可以取消");
        }
        reservation.setStatus(3); // cancelled
        reservationMapper.updateById(reservation);
        // 释放车位：高峰走 Redis 释放 + 异步回写 MySQL；非高峰走 MySQL CAS（保持原逻辑）
        ParkingSpot spot = parkingSpotMapper.selectById(reservation.getSpotId());
        boolean peak = spotCacheService.isPeakNow();
        Integer spotStatus = peak && spot != null ? spotCacheService.getStatus(spot.getId()) : (spot != null ? spot.getStatus() : null);
        if (spot != null && spotStatus != null && spotStatus == 2) {
            if (peak) {
                spotCacheService.release(spot.getId());
                spotCacheService.asyncSyncToDb(spot.getId(), 0);
                log.debug("Peak cancel: spot {} released via Redis, async sync to MySQL", spot.getId());
            } else {
                parkingSpotMapper.update(null,
                        new LambdaUpdateWrapper<ParkingSpot>()
                                .eq(ParkingSpot::getId, spot.getId())
                                .eq(ParkingSpot::getStatus, 2)
                                .set(ParkingSpot::getStatus, 0));
                spotCacheService.setStatus(spot.getId(), 0);
            }
        }
        spotRealtimeService.notifyChanged();
    }

    @Override
    public Reservation getById(Long id) {
        return reservationMapper.selectById(id);
    }

    /**
     * 只允许查看本人的预约详情。
     * 原来控制器直接调用 getById(id)，任何人都能用别人的预约 ID 拉到他人的车牌、手机号、金额（越权访问/IDOR）。
     */
    @Override
    public Reservation getMyReservation(Long userId, Long id) {
        Reservation reservation = reservationMapper.selectById(id);
        if (reservation == null) throw new BusinessException("预约记录不存在");
        if (userId == null || !reservation.getUserId().equals(userId)) {
            throw new BusinessException(403, "该预约不属于当前账号，无权查看");
        }
        return reservation;
    }
}
