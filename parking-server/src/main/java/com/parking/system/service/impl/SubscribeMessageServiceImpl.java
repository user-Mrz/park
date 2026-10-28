package com.parking.system.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.parking.system.dto.SubscribeReportDTO;
import com.parking.system.domain.MonthlyPass;
import com.parking.system.domain.Reservation;
import com.parking.system.domain.SubscribeAuth;
import com.parking.system.domain.User;
import com.parking.system.mapper.MonthlyPassMapper;
import com.parking.system.mapper.ReservationMapper;
import com.parking.system.mapper.SubscribeAuthMapper;
import com.parking.system.mapper.UserMapper;
import com.parking.system.service.CampusRuleService;
import com.parking.system.service.SubscribeMessageService;
import com.parking.common.util.WechatUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 微信订阅消息业务实现。
 *
 * <p>四类通知场景：
 * <ul>
 *   <li>reserve_remind — 预约生效前 15 分钟提醒（定时任务扫描 sendReserveRemind）</li>
 *   <li>balance_low    — 出场扣费后余额低于阈值时提醒（VehicleServiceImpl.exit 触发）</li>
 *   <li>identity_result — 管理员审核身份后提醒（AdminServiceImpl.updateUserIdentity 触发）</li>
 *   <li>monthly_pass_expire — 月卡到期前 7/3/1 天提醒（定时任务扫描 sendMonthlyPassExpire）</li>
 * </ul>
 * </p>
 *
 * <p>设计原则：所有发送调用均为"尽力而为"，失败只 log.error 不抛异常，
 * 不阻塞主业务流程；发送前必查 SubscribeAuth.subscribed=1 且 sendStatus=0，
 * 避免重复骚扰用户。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscribeMessageServiceImpl implements SubscribeMessageService {

    private final SubscribeAuthMapper subscribeAuthMapper;
    private final UserMapper userMapper;
    private final ReservationMapper reservationMapper;
    private final MonthlyPassMapper monthlyPassMapper;
    private final CampusRuleService campusRuleService;
    private final WechatUtil wechatUtil;

    private static final String SCENE_RESERVE_REMIND = "reserve_remind";
    private static final String SCENE_BALANCE_LOW = "balance_low";
    private static final String SCENE_IDENTITY_RESULT = "identity_result";
    private static final String SCENE_MONTHLY_PASS_EXPIRE = "monthly_pass_expire";
    /** 月卡到期提醒模板 ID 的系统配置键 */
    private static final String CONFIG_MONTHLY_PASS_EXPIRE_TEMPLATE = "wx_template_monthly_pass_expire";

    @Override
    @Transactional
    public void reportSubscription(Long userId, SubscribeReportDTO dto) {
        // 幂等：同一用户同一 scene 同一 bizRef 已落库则直接返回
        LambdaQueryWrapper<SubscribeAuth> dupCheck = new LambdaQueryWrapper<SubscribeAuth>()
                .eq(SubscribeAuth::getUserId, userId)
                .eq(SubscribeAuth::getScene, dto.getScene())
                .eq(SubscribeAuth::getTemplateId, dto.getTemplateId())
                .eq(StringUtils.hasText(dto.getBizRef()), SubscribeAuth::getBizRef, dto.getBizRef());
        if (subscribeAuthMapper.selectCount(dupCheck) > 0) {
            log.debug("[Subscribe] report 幂等跳过 userId={} scene={}", userId, dto.getScene());
            return;
        }
        Snowflake sf = IdUtil.getSnowflake(1, 8);
        SubscribeAuth auth = new SubscribeAuth();
        auth.setId(sf.nextId());
        auth.setUserId(userId);
        auth.setTemplateId(dto.getTemplateId());
        auth.setScene(dto.getScene());
        auth.setSubscribed(dto.getSubscribed());
        auth.setSendStatus(0);
        auth.setBizRef(dto.getBizRef() == null ? "" : dto.getBizRef());
        subscribeAuthMapper.insert(auth);
        log.info("[Subscribe] report userId={} scene={} subscribed={} bizRef={}",
                userId, dto.getScene(), dto.getSubscribed(), dto.getBizRef());
    }

    @Override
    public void sendReserveRemind(Reservation reservation) {
        if (reservation == null || reservation.getUserId() == null) return;
        User user = userMapper.selectById(reservation.getUserId());
        if (user == null || !StringUtils.hasText(user.getOpenid())) {
            log.info("[Subscribe] reserve_remind 跳过：openid 为空 reservationId={}", reservation.getId());
            return;
        }
        String templateId = templateMap().get(SCENE_RESERVE_REMIND);
        if (!checkAndMarkSent(reservation.getUserId(), SCENE_RESERVE_REMIND,
                reservation.getReservationNo(), templateId)) {
            return;
        }
        String startStr = reservation.getStartTime() == null ? ""
                : reservation.getStartTime().format(DateTimeFormatter.ofPattern("MM-dd HH:mm"));
        String endStr = reservation.getEndTime() == null ? ""
                : reservation.getEndTime().format(DateTimeFormatter.ofPattern("MM-dd HH:mm"));
        // 小程序跳转页面（预约详情）
        String page = "pages/reserve-detail/reserve-detail?id=" + reservation.getId();
        Map<String, Object> data = new HashMap<>();
        data.put("thing1", value("校园预约提醒"));
        data.put("time2", value(startStr + " - " + endStr));
        data.put("thing3", value("您预约的车位将在 15 分钟后生效，请准时入场"));
        boolean ok = wechatUtil.sendSubscribeMessage(user.getOpenid(), templateId, data, page);
        markSendResult(reservation.getUserId(), SCENE_RESERVE_REMIND,
                reservation.getReservationNo(), templateId, ok);
    }

    @Override
    public void sendBalanceLow(Long userId, BigDecimal newBalance) {
        User user = userMapper.selectById(userId);
        if (user == null || !StringUtils.hasText(user.getOpenid())) return;
        String templateId = templateMap().get(SCENE_BALANCE_LOW);
        if (!checkAndMarkSent(userId, SCENE_BALANCE_LOW, "BALANCE", templateId)) {
            return;
        }
        String page = "pages/recharge/recharge";
        Map<String, Object> data = new HashMap<>();
        data.put("thing1", value("校园停车余额提醒"));
        data.put("amount2", value(newBalance == null ? "0.00" : newBalance.toPlainString() + " 元"));
        data.put("thing3", value("余额不足 5 元，请及时充值以免影响下次停车"));
        boolean ok = wechatUtil.sendSubscribeMessage(user.getOpenid(), templateId, data, page);
        markSendResult(userId, SCENE_BALANCE_LOW, "BALANCE", templateId, ok);
    }

    @Override
    public void sendIdentityResult(Long userId, User user, int identityStatus, String remark) {
        if (user == null || !StringUtils.hasText(user.getOpenid())) return;
        String templateId = templateMap().get(SCENE_IDENTITY_RESULT);
        if (!checkAndMarkSent(userId, SCENE_IDENTITY_RESULT, String.valueOf(userId), templateId)) {
            return;
        }
        String statusLabel = identityStatus == 1 ? "审核通过" : (identityStatus == 2 ? "审核驳回" : "待审核");
        String page = "pages/profile/profile";
        Map<String, Object> data = new HashMap<>();
        data.put("thing1", value("校园身份认证结果"));
        data.put("thing2", value(statusLabel));
        data.put("thing3", value(StringUtils.hasText(remark) ? remark : "您的身份认证申请已处理"));
        boolean ok = wechatUtil.sendSubscribeMessage(user.getOpenid(), templateId, data, page);
        markSendResult(userId, SCENE_IDENTITY_RESULT, String.valueOf(userId), templateId, ok);
    }

    @Override
    public void sendMonthlyPassExpire(MonthlyPass pass, long daysLeft) {
        if (pass == null || pass.getUserId() == null) return;
        User user = userMapper.selectById(pass.getUserId());
        if (user == null || !StringUtils.hasText(user.getOpenid())) {
            log.info("[Subscribe] monthly_pass_expire 跳过：openid 为空 passId={}", pass.getId());
            return;
        }
        String templateId = templateMap().get(SCENE_MONTHLY_PASS_EXPIRE);
        // 每次里程碑单独授权、单独发送：bizRef = 月卡ID:剩余天数
        String bizRef = pass.getId() + ":" + daysLeft;
        if (!checkAndMarkSent(pass.getUserId(), SCENE_MONTHLY_PASS_EXPIRE, bizRef, templateId)) {
            return;
        }
        String expireDate = pass.getExpireDate() == null ? "" : pass.getExpireDate().toString();
        Map<String, Object> data = new HashMap<>();
        data.put("thing1", value("月卡即将到期"));
        data.put("time2", value(expireDate));
        data.put("thing3", value("您的月卡还有 " + daysLeft + " 天到期，请及时续费"));
        String page = "pages/my-pass/my-pass";
        boolean ok = wechatUtil.sendSubscribeMessage(user.getOpenid(), templateId, data, page);
        markSendResult(pass.getUserId(), SCENE_MONTHLY_PASS_EXPIRE, bizRef, templateId, ok);
    }

    @Override
    public Map<String, String> templateMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(SCENE_RESERVE_REMIND, campusRuleService.config("wx_template_reserve_remind", ""));
        map.put(SCENE_BALANCE_LOW, campusRuleService.config("wx_template_balance_low", ""));
        map.put(SCENE_IDENTITY_RESULT, campusRuleService.config("wx_template_identity_result", ""));
        map.put(SCENE_MONTHLY_PASS_EXPIRE,
                campusRuleService.config(CONFIG_MONTHLY_PASS_EXPIRE_TEMPLATE, ""));
        return map;
    }

    /**
     * 每日 09:00 扫描到期前 7/3/1 天的生效月卡，按用户授权情况发送续费提醒。
     * 未授权、模板未配置、openid 为空时静默跳过，不影响主业务。
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void scanExpiringMonthlyPasses() {
        LocalDate today = LocalDate.now();
        List<LocalDate> targetDates = List.of(
                today.plusDays(7), today.plusDays(3), today.plusDays(1));
        List<MonthlyPass> passes = monthlyPassMapper.selectList(new LambdaQueryWrapper<MonthlyPass>()
                .eq(MonthlyPass::getStatus, 1)
                .in(MonthlyPass::getExpireDate, targetDates));
        for (MonthlyPass pass : passes) {
            try {
                long daysLeft = ChronoUnit.DAYS.between(today, pass.getExpireDate());
                sendMonthlyPassExpire(pass, daysLeft);
            } catch (Exception e) {
                log.error("[Subscribe] scanExpiringMonthlyPasses 异常 passId={} error={}",
                        pass.getId(), e.getMessage());
            }
        }
    }

    // ---- 定时任务：每分钟扫描「未来 15 分钟内开始且未发送提醒」的预约 ----
    @Scheduled(cron = "0 * * * * ?")
    public void scanUpcomingReservations() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startWindow = now.plusMinutes(14);   // 给执行留 1 分钟窗口，避免边界漏掉
        LocalDateTime endWindow = now.plusMinutes(16);
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getStatus, 1)
                .ge(Reservation::getStartTime, startWindow)
                .le(Reservation::getStartTime, endWindow);
        for (Reservation r : reservationMapper.selectList(wrapper)) {
            try {
                sendReserveRemind(r);
            } catch (Exception e) {
                log.error("[Subscribe] scanUpcomingReservations 异常 reservationId={} error={}",
                        r.getId(), e.getMessage());
            }
        }
    }

    // ---- 内部工具 ----

    /** 返回微信模板 data 要求的 {"value": "xxx"} 包装 */
    private Map<String, Object> value(String v) {
        Map<String, Object> m = new HashMap<>();
        m.put("value", v == null ? "" : v);
        return m;
    }

    /**
     * 原子判定「该用户该场景该 bizRef 是否已同意订阅且尚未发送」。
     * @return true 表示可以发送，false 表示跳过
     */
    private boolean checkAndMarkSent(Long userId, String scene, String bizRef, String templateId) {
        if (!StringUtils.hasText(templateId)) return false;
        LambdaQueryWrapper<SubscribeAuth> w = new LambdaQueryWrapper<SubscribeAuth>()
                .eq(SubscribeAuth::getUserId, userId)
                .eq(SubscribeAuth::getScene, scene)
                .eq(StringUtils.hasText(bizRef), SubscribeAuth::getBizRef, bizRef)
                .eq(SubscribeAuth::getSubscribed, 1)
                .eq(SubscribeAuth::getSendStatus, 0)
                .last("LIMIT 1");
        SubscribeAuth auth = subscribeAuthMapper.selectOne(w);
        if (auth == null) {
            log.info("[Subscribe] 跳过：没有 userId={} scene={} bizRef={} 的未发送授权记录",
                    userId, scene, bizRef);
            return false;
        }
        return true;
    }

    private void markSendResult(Long userId, String scene, String bizRef, String templateId, boolean ok) {
        LambdaUpdateWrapper<SubscribeAuth> w = new LambdaUpdateWrapper<SubscribeAuth>()
                .eq(SubscribeAuth::getUserId, userId)
                .eq(SubscribeAuth::getScene, scene)
                .eq(StringUtils.hasText(bizRef), SubscribeAuth::getBizRef, bizRef);
        subscribeAuthMapper.update(null, w
                .set(SubscribeAuth::getSendStatus, ok ? 1 : 2)
                .set(SubscribeAuth::getSendTime, LocalDateTime.now())
                .set(SubscribeAuth::getUpdatedAt, LocalDateTime.now()));
    }
}
