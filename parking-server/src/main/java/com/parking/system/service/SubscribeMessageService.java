package com.parking.system.service;

import com.parking.system.dto.SubscribeReportDTO;
import com.parking.system.domain.MonthlyPass;
import com.parking.system.domain.Reservation;
import com.parking.system.domain.User;

import java.util.Map;

/**
 * 微信订阅消息服务。
 */
public interface SubscribeMessageService {

    /** 前端授权结果上报：落库 t_subscribe_auth */
    void reportSubscription(Long userId, SubscribeReportDTO dto);

    /** 发送预约前 15 分钟提醒（定时任务调用） */
    void sendReserveRemind(Reservation reservation);

    /** 发送余额不足提醒（扣费后触发） */
    void sendBalanceLow(Long userId, java.math.BigDecimal newBalance);

    /** 发送身份审核结果提醒（管理员审核通过/驳回时触发） */
    void sendIdentityResult(Long userId, User user, int identityStatus, String remark);

    /** 发送月卡到期前提醒（定时任务在到期前 7/3/1 天触发） */
    void sendMonthlyPassExpire(MonthlyPass pass, long daysLeft);

    /** 返回 4 类场景对应的模板 ID（前端 requestSubscribeMessage 需要） */
    Map<String, String> templateMap();
}
