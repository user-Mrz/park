package com.parking.service;

import com.parking.dto.VehicleEnterDTO;
import com.parking.entity.VehicleRecord;

public interface VehicleService {
    VehicleRecord enter(Long userId, VehicleEnterDTO dto);
    VehicleRecord exit(Long userId, Long recordId);
    VehicleRecord getCurrentParking(Long userId);
}
