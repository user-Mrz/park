package com.parking.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parking.dto.BlacklistDTO;
import com.parking.entity.Blacklist;

import java.util.Map;

/**
 * 校园化管理：统计报表、黑名单、CSV 导出
 */
public interface CampusAdminService {

    /** 校园统计：按身份、按院系、按分区汇总 */
    Map<String, Object> campusStats();

    /** 黑名单列表 */
    Page<Blacklist> listBlacklist(int page, int pageSize, String keyword);

    /** 新增黑名单 */
    Blacklist addBlacklist(BlacklistDTO dto);

    /** 移除黑名单 */
    void removeBlacklist(Long id);

    /** 导出 CSV 内容（utf-8，含表头） */
    String exportCsv(String type);
}
