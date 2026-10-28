package com.parking.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parking.system.dto.VehicleEnterDTO;
import com.parking.system.domain.VehicleRecord;
import com.parking.system.domain.Violation;

public interface VehicleService {
    VehicleRecord enter(Long userId, VehicleEnterDTO dto);
    VehicleRecord exit(Long userId, Long recordId);
    /** 管理员强制出场：按校园费率结算、自动扣费并释放车位 */
    VehicleRecord forceExit(Long recordId);
    VehicleRecord getCurrentParking(Long userId);
    /** 用户端：我的违规记录 */
    Page<Violation> getMyViolations(Long userId, int page, int pageSize);
}
