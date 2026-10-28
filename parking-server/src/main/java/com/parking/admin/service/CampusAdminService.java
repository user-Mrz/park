package com.parking.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parking.system.dto.BlacklistDTO;
import com.parking.system.dto.WhitelistDTO;
import com.parking.system.domain.Blacklist;
import com.parking.system.domain.Whitelist;

import java.time.LocalDate;
import java.util.Map;

/**
 * 校园化管理：统计报表、黑白名单、CSV 导出
 */
public interface CampusAdminService {

    /** 校园统计：按身份、按院系、按分区汇总 */
    Map<String, Object> campusStats();

    /** 区间日统计：按自然日汇总收入与入场车次（首页柱状图） */
    Map<String, Object> dailyStats(LocalDate startDate, LocalDate endDate);

    /** 黑名单列表（status：active/expired/all；source：manual/auto_violation） */
    Page<Blacklist> listBlacklist(int page, int pageSize, String keyword, String status, String source);

    /** 新增黑名单 */
    Blacklist addBlacklist(BlacklistDTO dto, Long operatorId);

    /** 移除黑名单 */
    void removeBlacklist(Long id);

    /** 白名单列表（status：active/expired/all） */
    Page<Whitelist> listWhitelist(int page, int pageSize, String keyword, String status);

    /** 新增白名单 */
    Whitelist addWhitelist(WhitelistDTO dto, Long operatorId);

    /** 移除白名单 */
    void removeWhitelist(Long id);

    /** 导出 CSV 内容（utf-8，含表头） */
    String exportCsv(String type);
}
