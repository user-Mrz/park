package com.parking.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parking.dto.RechargeDTO;
import com.parking.dto.UpdateUserDTO;
import com.parking.entity.PaymentRecord;
import com.parking.entity.RechargeRecord;
import com.parking.entity.Reservation;
import com.parking.entity.User;
import com.parking.entity.VehicleRecord;

public interface UserService {
    User getById(Long id);
    User getByPhone(String phone);
    void updateInfo(Long userId, UpdateUserDTO dto);
    RechargeRecord recharge(Long userId, RechargeDTO dto);
    Page<RechargeRecord> getRechargeRecords(Long userId, int page, int pageSize);
    Page<PaymentRecord> getPaymentRecords(Long userId, int page, int pageSize);
    Page<Reservation> getUserReservations(Long userId, int page, int pageSize);
    Page<VehicleRecord> getUserVehicleRecords(Long userId, int page, int pageSize);
}
