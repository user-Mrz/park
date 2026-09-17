package com.parking.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.parking.common.BusinessException;
import com.parking.dto.ReservationDTO;
import com.parking.entity.ParkingSpot;
import com.parking.entity.Reservation;
import com.parking.entity.User;
import com.parking.mapper.ParkingSpotMapper;
import com.parking.mapper.ReservationMapper;
import com.parking.mapper.UserMapper;
import com.parking.service.CampusRuleService;
import com.parking.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private final ReservationMapper reservationMapper;
    private final ParkingSpotMapper parkingSpotMapper;
    private final CampusRuleService campusRuleService;
    private final UserMapper userMapper;

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
        // CAS: 原子更新，只有空闲车位才能被预约
        if (spot.getStatus() != 0) throw new BusinessException("该车位当前不可预约");
        int rows = parkingSpotMapper.update(null,
                new LambdaUpdateWrapper<ParkingSpot>()
                        .eq(ParkingSpot::getId, dto.getSpotId())
                        .eq(ParkingSpot::getStatus, 0)  // 只有空闲状态才能预约
                        .set(ParkingSpot::getStatus, 2));
        if (rows == 0) throw new BusinessException("该车位刚被他人预约，请重新选择");
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
        return reservation;
    }

    @Override
    @Transactional
    public void cancelReservation(Long userId, Long reservationId) {
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) throw new BusinessException("Reservation not found");
        if (!reservation.getUserId().equals(userId)) throw new BusinessException("Not your reservation");
        if (reservation.getStatus() != 1) {
            throw new BusinessException("Only active reservations can be cancelled");
        }
        reservation.setStatus(3); // cancelled
        reservationMapper.updateById(reservation);
        // CAS: 只有该预约占用的车位才释放
        ParkingSpot spot = parkingSpotMapper.selectById(reservation.getSpotId());
        if (spot != null && spot.getStatus() == 2) {
            parkingSpotMapper.update(null,
                    new LambdaUpdateWrapper<ParkingSpot>()
                            .eq(ParkingSpot::getId, spot.getId())
                            .eq(ParkingSpot::getStatus, 2)
                            .set(ParkingSpot::getStatus, 0));
        }
    }

    @Override
    public Reservation getById(Long id) {
        return reservationMapper.selectById(id);
    }
}
