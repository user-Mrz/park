package com.parking.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parking.system.dto.VisitReportDTO;
import com.parking.system.domain.VisitReport;

import java.util.Map;

/**
 * 访客与家长车辆报备服务
 */
public interface VisitReportService {

    /** 师生提交报备（家长接送 / 访客来访） */
    VisitReport create(Long ownerUserId, VisitReportDTO dto);

    /** 我的报备记录 */
    Page<VisitReport> myReports(Long ownerUserId, int page, int pageSize);

    /** 撤销报备（仅待审核/已通过状态） */
    void cancel(Long ownerUserId, Long reportId);

    /** 管理端：报备列表 */
    Page<VisitReport> listAll(int page, int pageSize, Integer status, String keyword);

    /** 管理端：审核通过（自动创建访客账号并分配访客区车位） */
    VisitReport approve(Long reportId, Long reviewerId, Long spotId, String remark);

    /** 管理端：驳回 */
    VisitReport reject(Long reportId, Long reviewerId, String remark);

    /** 管理端：登记入场（访客到达校门，创建停车记录） */
    VisitReport checkIn(Long reportId);

    /** 管理端：出场结算（复用强制出场逻辑，按身份免费时长与费率结算） */
    VisitReport checkOut(Long reportId);

    /** 报备统计（管理端看板用） */
    Map<String, Object> stats();
}
