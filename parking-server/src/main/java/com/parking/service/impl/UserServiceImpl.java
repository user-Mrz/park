package com.parking.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parking.common.BusinessException;
import com.parking.dto.RechargeDTO;
import com.parking.dto.UpdateUserDTO;
import com.parking.entity.PaymentRecord;
import com.parking.entity.RechargeRecord;
import com.parking.entity.Reservation;
import com.parking.entity.User;
import com.parking.entity.VehicleRecord;
import com.parking.mapper.PaymentRecordMapper;
import com.parking.mapper.RechargeRecordMapper;
import com.parking.mapper.ReservationMapper;
import com.parking.mapper.UserMapper;
import com.parking.mapper.VehicleRecordMapper;
import com.parking.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final RechargeRecordMapper rechargeRecordMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final ReservationMapper reservationMapper;
    private final VehicleRecordMapper vehicleRecordMapper;

    @Override
    public User getById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException("User not found");
        return user;
    }

    @Override
    public User getByPhone(String phone) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
    }

    @Override
    public void updateInfo(Long userId, UpdateUserDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("User not found");
        if (dto.getNickname() != null) user.setNickname(dto.getNickname());
        if (dto.getPlateNumber() != null) user.setPlateNumber(dto.getPlateNumber());
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public RechargeRecord recharge(Long userId, RechargeDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("User not found");
        Snowflake sf = IdUtil.getSnowflake(1, 2);
        RechargeRecord record = new RechargeRecord();
        record.setId(sf.nextId());
        record.setUserId(userId);
        record.setOrderNo("RECH" + System.currentTimeMillis());
        record.setAmount(dto.getAmount());
        record.setPayMethod(dto.getPayMethod() != null ? dto.getPayMethod() : "wechat");
        record.setStatus(1); // success in demo
        rechargeRecordMapper.insert(record);
        user.setBalance(user.getBalance().add(dto.getAmount()));
        userMapper.updateById(user);
        return record;
    }

    @Override
    public Page<RechargeRecord> getRechargeRecords(Long userId, int page, int pageSize) {
        LambdaQueryWrapper<RechargeRecord> wrapper = new LambdaQueryWrapper<RechargeRecord>()
                .eq(RechargeRecord::getUserId, userId)
                .orderByDesc(RechargeRecord::getCreatedAt);
        return rechargeRecordMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public Page<PaymentRecord> getPaymentRecords(Long userId, int page, int pageSize) {
        LambdaQueryWrapper<PaymentRecord> wrapper = new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getUserId, userId)
                .orderByDesc(PaymentRecord::getCreatedAt);
        return paymentRecordMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public Page<Reservation> getUserReservations(Long userId, int page, int pageSize) {
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getUserId, userId)
                .orderByDesc(Reservation::getCreatedAt);
        return reservationMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public Page<VehicleRecord> getUserVehicleRecords(Long userId, int page, int pageSize) {
        LambdaQueryWrapper<VehicleRecord> wrapper = new LambdaQueryWrapper<VehicleRecord>()
                .eq(VehicleRecord::getUserId, userId)
                .orderByDesc(VehicleRecord::getCreatedAt);
        return vehicleRecordMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }
}
