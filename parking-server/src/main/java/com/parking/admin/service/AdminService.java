package com.parking.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parking.system.domain.*;

import java.util.Map;

public interface AdminService {
    // User management
    Page<User> getUsers(int page, int pageSize, String keyword, Integer status, Integer identityType, Integer identityStatus);
    /** 用户详情：账号资料 + 业务统计（停车/预约/充值/消费） */
    Map<String, Object> getUserById(Long id);
    void updateUserStatus(Long id, Integer status);
    /** 校园身份审核：1通过 2驳回 0改为待审核 */
    void updateUserIdentity(Long id, Integer identityStatus, String remark);
    java.math.BigDecimal getTotalUserBalance();
    // Reservation management
    Page<Reservation> getAllReservations(int page, int pageSize, Integer status,String reservationNo, Long userId, Long spotId);
    void updateReservationStatus(Long id, Integer status);
    // Vehicle records
    Page<VehicleRecord> getAllVehicleRecords(int page, int pageSize, Integer status,
                                               String plateNumber, Long userId, Long spotId);
    VehicleRecord updateVehicleRecord(Long id, VehicleRecord record);
    // Recharge records
    Page<RechargeRecord> getAllRechargeRecords(int page, int pageSize);
    // Payment records
    Page<PaymentRecord> getAllPaymentRecords(int page, int pageSize);
    // Monthly pass（月卡 / 长租）
    /** 月卡记录分页：keyword 匹配车牌 / 昵称 / 手机号 / 学号工号；status 1生效 2过期 3退款 */
    Page<MonthlyPass> getMonthlyPasses(int page, int pageSize, String keyword, Integer status, Integer passType);
    /** 月卡经营概览：生效中 / 7 天内到期 / 已过期 / 累计与本月长租收入 */
    Map<String, Object> getMonthlyPassStats();
    // Parking spots
    /** spotType 为 null 时查询全部；0 固定车位；1 潮汐临时车位 */
    Page<ParkingSpot> getAllSpots(int page, int pageSize, Integer spotType, String spotCode, String area, String zoneCode);
    ParkingSpot addSpot(ParkingSpot spot);
    ParkingSpot updateSpot(Long id, ParkingSpot spot);
    void updateSpotStatus(Long id, Integer status);
    /** 删除车位：占用/预约中的车位不允许删除 */
    void deleteSpot(Long id);
    // System config
    Map<String, String> getAllConfig();
    void updateConfig(Map<String, String> configs);
    // Dashboard
    Map<String, Object> getDashboard();
    // Spot Map
    Map<String, Object> getSpotMap();
}
