package com.parking.miniapp.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parking.system.dto.MonthlyPassOpenDTO;
import com.parking.system.domain.MonthlyPass;

import java.util.Map;

/**
 * 月卡 / 长租停车权益服务。
 */
public interface MonthlyPassService {

    /** 开通或续费月卡；扣费逻辑走用户余额，余额不足拒绝开通 */
    MonthlyPass open(Long userId, MonthlyPassOpenDTO dto);

    /** 分页查询本人月卡记录 */
    Page<MonthlyPass> listMy(Long userId, int page, int pageSize);

    /** 查询当前有效月卡（单条）；返回 null 表示当前没有生效的月卡 */
    MonthlyPass current(Long userId);

    /** 返回月卡价格配置（月 / 季 / 年），供前端展示 */
    Map<String, Object> priceConfig();
}
