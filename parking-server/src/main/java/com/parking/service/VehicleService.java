package com.parking.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parking.dto.VehicleEnterDTO;
import com.parking.entity.VehicleRecord;
import com.parking.entity.Violation;

public interface VehicleService {
    VehicleRecord enter(Long userId, VehicleEnterDTO dto);
    VehicleRecord exit(Long userId, Long recordId);
    VehicleRecord getCurrentParking(Long userId);
    /** 用户端：我的违规记录 */
    Page<Violation> getMyViolations(Long userId, int page, int pageSize);
}
