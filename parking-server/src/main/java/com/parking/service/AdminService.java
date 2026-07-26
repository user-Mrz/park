package com.parking.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parking.entity.*;

import java.util.Map;

public interface AdminService {
    // User management
    Page<User> getUsers(int page, int pageSize, String keyword, Integer status);
    User getUserById(Long id);
    void updateUserStatus(Long id, Integer status);
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
}
