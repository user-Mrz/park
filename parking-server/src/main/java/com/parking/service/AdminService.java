package com.parking.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parking.entity.*;

import java.util.Map;

public interface AdminService {
    // User management
    Page<User> getUsers(int page, int pageSize, String keyword, Integer status,
                        Integer identityType, Integer identityStatus);
    /** 用户详情：账号资料 + 业务统计（停车/预约/充值/消费） */
    Map<String, Object> getUserById(Long id);
    void updateUserStatus(Long id, Integer status);
    /** 校园身份审核：1通过 2驳回 0改为待审核 */
    void updateUserIdentity(Long id, Integer identityStatus, String remark);
    java.math.BigDecimal getTotalUserBalance();
    // Reservation management
    Page<Reservation> getAllReservations(int page, int pageSize, Integer status);
    void updateReservationStatus(Long id, Integer status);
    // Vehicle records
    Page<VehicleRecord> getAllVehicleRecords(int page, int pageSize, Integer status);
    VehicleRecord updateVehicleRecord(Long id, VehicleRecord record);
    VehicleRecord forceExit(Long id);
    // Recharge records
    Page<RechargeRecord> getAllRechargeRecords(int page, int pageSize);
    // Payment records
    Page<PaymentRecord> getAllPaymentRecords(int page, int pageSize);
    // Parking spots
    Page<ParkingSpot> getAllSpots(int page, int pageSize);
    ParkingSpot addSpot(ParkingSpot spot);
    ParkingSpot updateSpot(Long id, ParkingSpot spot);
    void updateSpotStatus(Long id, Integer status);
    // System config
    Map<String, String> getAllConfig();
    void updateConfig(Map<String, String> configs);
    // Dashboard
    Map<String, Object> getDashboard();
    // Spot Map
    Map<String, Object> getSpotMap();
}
