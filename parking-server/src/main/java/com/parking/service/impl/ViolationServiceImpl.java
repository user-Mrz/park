package com.parking.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parking.common.BusinessException;
import com.parking.entity.Violation;
import com.parking.mapper.ViolationMapper;
import com.parking.service.CampusRuleService;
import com.parking.service.ViolationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ViolationServiceImpl implements ViolationService {

    private final ViolationMapper violationMapper;
    private final CampusRuleService campusRuleService;

    @Override
    public void checkOvertime(Long userId, String plateNumber, Long recordId, Long spotId,
                             LocalDateTime deadline, LocalDateTime exitTime, String scene) {
        if (deadline == null || exitTime == null) return;
        int grace = campusRuleService.intConfig("violation_overtime_grace_minutes", 30);
        LocalDateTime allowed = deadline.plusMinutes(grace);
        if (!exitTime.isAfter(allowed)) return;   // 宽限期内不算违规

        long overMinutes = Duration.between(allowed, exitTime).toMinutes();
        Snowflake sf = IdUtil.getSnowflake(1, 9);
        Violation v = new Violation();
        v.setId(sf.nextId());
        v.setUserId(userId);
        v.setPlateNumber(plateNumber);
        v.setVehicleRecordId(recordId);
        v.setSpotId(spotId);
        v.setType("overtime");
        v.setDescription("超时占用 " + overMinutes + " 分钟（" + (scene == null ? "停车" : scene) + "）");
        v.setPoints(overMinutes > 120 ? 2 : 1);
        v.setFineAmount(BigDecimal.ZERO);
        v.setStatus(0);
        v.setCreatedAt(LocalDateTime.now());
        violationMapper.insert(v);
    }

    /**
     * 违规区域入场：调用方（如入场校验）会紧接着抛出业务异常拒绝入场，
     * 因此这里必须使用独立事务（REQUIRES_NEW），否则违规记录会随外层事务一起回滚。
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordIllegalZone(Long userId, String plateNumber, Long spotId, String description) {
        Snowflake sf = IdUtil.getSnowflake(1, 9);
        Violation v = new Violation();
        v.setId(sf.nextId());
        v.setUserId(userId);
        v.setPlateNumber(plateNumber);
        v.setSpotId(spotId);
        v.setType("illegal_zone");
        v.setDescription("违规区域入场：" + (description == null ? "" : description));
        v.setPoints(2);
        v.setFineAmount(BigDecimal.ZERO);
        v.setStatus(0);
        v.setCreatedAt(LocalDateTime.now());
        violationMapper.insert(v);
    }

    @Override
    public Page<Violation> myViolations(Long userId, int page, int pageSize) {
        return violationMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<Violation>()
                        .eq(Violation::getUserId, userId)
                        .orderByDesc(Violation::getCreatedAt));
    }

    @Override
    public Page<Violation> list(int page, int pageSize, Integer status, String keyword) {
        LambdaQueryWrapper<Violation> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(Violation::getStatus, status);
        if (StringUtils.hasText(keyword)) wrapper.like(Violation::getPlateNumber, keyword);
        wrapper.orderByDesc(Violation::getCreatedAt);
        return violationMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public void handle(Long id, Long operatorId, String remark) {
        Violation v = violationMapper.selectById(id);
        if (v == null) throw new BusinessException("违规记录不存在");
        v.setStatus(1);
        v.setOperatorId(operatorId);
        v.setHandledAt(LocalDateTime.now());
        if (StringUtils.hasText(remark)) {
            v.setDescription((v.getDescription() == null ? "" : v.getDescription()) + " | 处理意见：" + remark.trim());
        }
        violationMapper.updateById(v);
    }

    @Override
    public Map<String, Object> stats() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", count(null));
        result.put("pending", count(0));
        result.put("handled", count(1));
        return result;
    }

    private long count(Integer status) {
        LambdaQueryWrapper<Violation> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(Violation::getStatus, status);
        Long c = violationMapper.selectCount(wrapper);
        return c == null ? 0 : c;
    }
}
