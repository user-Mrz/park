package com.parking.miniapp.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parking.common.BusinessException;
import com.parking.system.dto.MonthlyPassOpenDTO;
import com.parking.system.domain.MonthlyPass;
import com.parking.system.domain.PaymentRecord;
import com.parking.system.domain.User;
import com.parking.system.mapper.MonthlyPassMapper;
import com.parking.system.mapper.PaymentRecordMapper;
import com.parking.system.mapper.UserMapper;
import com.parking.system.service.CampusRuleService;
import com.parking.miniapp.service.MonthlyPassService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 月卡 / 长租停车权益实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonthlyPassServiceImpl implements MonthlyPassService {

    private final MonthlyPassMapper monthlyPassMapper;
    private final UserMapper userMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final CampusRuleService campusRuleService;

    @Override
    @Transactional
    public MonthlyPass open(Long userId, MonthlyPassOpenDTO dto) {
        if (dto.getPassType() == null || dto.getPassType() < 1 || dto.getPassType() > 3) {
            throw new BusinessException("卡类型不合法（1月卡/2季卡/3年卡）");
        }
        // 对用户行加悲观锁，串行化同一用户的开通/续费请求：
        // 避免并发下两个事务读到同一张生效月卡、各自扣款却只延长一次有效期
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getId, userId).last("FOR UPDATE"));
        if (user == null) throw new BusinessException("用户不存在");

        String plate = dto.getPlateNumber() != null && !dto.getPlateNumber().isBlank()
                ? dto.getPlateNumber().trim().toUpperCase()
                : user.getPlateNumber();
        if (!StringUtils.hasText(plate)) {
            throw new BusinessException("请先在「账号信息」中绑定车牌号，或在开通时显式指定车牌");
        }

        LocalDate startDate;
        if (StringUtils.hasText(dto.getStartDate())) {
            try {
                startDate = LocalDate.parse(dto.getStartDate().trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (Exception e) {
                throw new BusinessException("起始日期格式错误，请使用 yyyy-MM-dd");
            }
        } else {
            startDate = LocalDate.now();
        }

        LocalDate expireDate = startDate.plusMonths(monthsOf(dto.getPassType()));

        BigDecimal price = priceOf(dto.getPassType());
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("月卡价格未配置，请联系管理员");
        }
        if (user.getBalance() == null || user.getBalance().compareTo(price) < 0) {
            throw new BusinessException("余额不足，开通该卡需要 " + price + " 元，请先充值");
        }

        // 如果用户已拥有同类型的生效月卡，自动叠加到期日（续费）
        MonthlyPass existing = monthlyPassMapper.selectOne(new LambdaQueryWrapper<MonthlyPass>()
                .eq(MonthlyPass::getUserId, userId)
                .eq(MonthlyPass::getPlateNumber, plate)
                .eq(MonthlyPass::getStatus, 1)
                .orderByDesc(MonthlyPass::getExpireDate)
                .last("LIMIT 1"));
        if (existing != null && existing.getExpireDate() != null && existing.getExpireDate().isAfter(LocalDate.now())) {
            // 续费：新的到期日 = 当前到期日 + 时长
            LocalDate newStart = existing.getExpireDate().plusDays(1);
            LocalDate newExpire = newStart.plusMonths(monthsOf(dto.getPassType()));
            existing.setStartDate(newStart);
            existing.setExpireDate(newExpire);
            existing.setPricePaid(price);
            existing.setZoneCode(dto.getZoneCode() == null ? "" : dto.getZoneCode());
            existing.setUpdatedAt(LocalDateTime.now());
            monthlyPassMapper.updateById(existing);
            BigDecimal balanceBefore = user.getBalance() == null ? BigDecimal.ZERO : user.getBalance();
            BigDecimal balanceAfter = deductBalance(userId, price);
            recordPassPayment(userId, price, balanceBefore, balanceAfter);
            fillDisplay(existing);
            log.info("续费月卡 userId={} plate={} passType={} expireDate={}", userId, plate, dto.getPassType(), newExpire);
            return existing;
        }

        // 新开通
        Snowflake sf = IdUtil.getSnowflake(1, 7);
        MonthlyPass pass = new MonthlyPass();
        pass.setId(sf.nextId());
        pass.setUserId(userId);
        pass.setPlateNumber(plate);
        pass.setPassType(dto.getPassType());
        pass.setZoneCode(dto.getZoneCode() == null ? "" : dto.getZoneCode());
        pass.setStartDate(startDate);
        pass.setExpireDate(expireDate);
        pass.setPricePaid(price);
        pass.setStatus(1);
        monthlyPassMapper.insert(pass);
        BigDecimal balanceBefore = user.getBalance() == null ? BigDecimal.ZERO : user.getBalance();
        BigDecimal balanceAfter = deductBalance(userId, price);
        recordPassPayment(userId, price, balanceBefore, balanceAfter);
        fillDisplay(pass);
        log.info("新开通月卡 userId={} plate={} passType={} expireDate={} price={}",
                userId, plate, dto.getPassType(), expireDate, price);
        return pass;
    }

    @Override
    public Page<MonthlyPass> listMy(Long userId, int page, int pageSize) {
        Page<MonthlyPass> pager = monthlyPassMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<MonthlyPass>()
                        .eq(MonthlyPass::getUserId, userId)
                        .orderByDesc(MonthlyPass::getCreatedAt));
        for (MonthlyPass m : pager.getRecords()) fillDisplay(m);
        return pager;
    }

    @Override
    public MonthlyPass current(Long userId) {
        MonthlyPass pass = monthlyPassMapper.selectOne(new LambdaQueryWrapper<MonthlyPass>()
                .eq(MonthlyPass::getUserId, userId)
                .eq(MonthlyPass::getStatus, 1)
                .ge(MonthlyPass::getExpireDate, LocalDate.now())
                .orderByAsc(MonthlyPass::getExpireDate)
                .last("LIMIT 1"));
        if (pass != null) fillDisplay(pass);
        return pass;
    }

    @Override
    public Map<String, Object> priceConfig() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("monthly", campusRuleService.decimalConfig("price_monthly_pass", new BigDecimal("199.00")));
        result.put("quarterly", campusRuleService.decimalConfig("price_quarterly_pass", new BigDecimal("499.00")));
        result.put("yearly", campusRuleService.decimalConfig("price_yearly_pass", new BigDecimal("1599.00")));
        result.put("monthlyLabel", "月卡 30 天");
        result.put("quarterlyLabel", "季卡 90 天");
        result.put("yearlyLabel", "年卡 365 天");
        return result;
    }

    /** 扣减余额并返回扣后余额（调用方已持有用户行锁，扣后余额查询可靠） */
    private BigDecimal deductBalance(Long userId, BigDecimal amount) {
        int rows = userMapper.deductBalance(userId, amount);
        if (rows == 0) throw new BusinessException("扣费失败，请确认余额是否充足");
        User after = userMapper.selectById(userId);
        return after == null || after.getBalance() == null ? BigDecimal.ZERO : after.getBalance();
    }

    /**
     * 把月卡消费写入扣费记录（t_payment_record）。
     * 月卡属于非停车消费，vehicleRecordId 为空，type 固定为 monthly_pass。
     */
    private void recordPassPayment(Long userId, BigDecimal amount,
                                   BigDecimal balanceBefore, BigDecimal balanceAfter) {
        Snowflake sf = IdUtil.getSnowflake(1, 9);
        PaymentRecord payment = new PaymentRecord();
        payment.setId(sf.nextId());
        payment.setUserId(userId);
        payment.setVehicleRecordId(null);
        payment.setAmount(amount);
        payment.setBalanceBefore(balanceBefore);
        payment.setBalanceAfter(balanceAfter);
        payment.setType("monthly_pass");
        paymentRecordMapper.insert(payment);
        log.info("月卡扣费流水 userId={} amount={} balance={}->{}", userId, amount, balanceBefore, balanceAfter);
    }

    private int monthsOf(Integer passType) {
        switch (passType) {
            case 2: return 3;
            case 3: return 12;
            default: return 1;
        }
    }

    private BigDecimal priceOf(Integer passType) {
        switch (passType) {
            case 2: return campusRuleService.decimalConfig("price_quarterly_pass", new BigDecimal("499.00"));
            case 3: return campusRuleService.decimalConfig("price_yearly_pass", new BigDecimal("1599.00"));
            default: return campusRuleService.decimalConfig("price_monthly_pass", new BigDecimal("199.00"));
        }
    }

    private void fillDisplay(MonthlyPass pass) {
        if (pass == null) return;
        switch (pass.getPassType() == null ? 0 : pass.getPassType()) {
            case 1: pass.setPassTypeLabel("月卡（30 天）"); break;
            case 2: pass.setPassTypeLabel("季卡（90 天）"); break;
            case 3: pass.setPassTypeLabel("年卡（365 天）"); break;
            default: pass.setPassTypeLabel("未知");
        }
        if (pass.getExpireDate() != null) {
            long days = ChronoUnit.DAYS.between(LocalDate.now(), pass.getExpireDate());
            pass.setRemainingDays(Math.max(0, days));
        }
    }
}
