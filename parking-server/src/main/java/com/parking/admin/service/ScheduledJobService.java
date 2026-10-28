package com.parking.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parking.system.domain.ScheduledJob;
import com.parking.system.dto.ScheduledJobDTO;

import java.util.List;
import java.util.Map;

/**
 * 定时任务管理：增删改查、启停、立即执行。
 */
public interface ScheduledJobService {

    Page<ScheduledJob> page(int page, int pageSize, String keyword, Integer status);

    /** 可选的执行器白名单（供前端下拉选择） */
    List<Map<String, Object>> handlers();

    ScheduledJob create(ScheduledJobDTO dto);

    ScheduledJob update(Long id, ScheduledJobDTO dto);

    void delete(Long id);

    /** status：0 暂停，1 运行 */
    void changeStatus(Long id, Integer status);

    /** 立即执行一次（异步，不影响原有 Cron 调度） */
    void runOnce(Long id);
}
