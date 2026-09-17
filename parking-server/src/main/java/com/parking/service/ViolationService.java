package com.parking.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parking.entity.Violation;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 违规记录服务（校园超时占用、违规停放等）
 */
public interface ViolationService {

    /** 出场时判定是否超时并自动记录违规 */
    void checkOvertime(Long userId, String plateNumber, Long recordId, Long spotId,
                       LocalDateTime deadline, LocalDateTime exitTime, String scene);

    /** 违规区域入场（违反校园分区规则）自动记录违规 */
    void recordIllegalZone(Long userId, String plateNumber, Long spotId, String description);

    /** 用户端：我的违规记录 */
    Page<Violation> myViolations(Long userId, int page, int pageSize);

    Page<Violation> list(int page, int pageSize, Integer status, String keyword);

    void handle(Long id, Long operatorId, String remark);

    Map<String, Object> stats();
}
