package com.parking.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parking.system.domain.PaymentRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface PaymentRecordMapper extends BaseMapper<PaymentRecord> {

    /** 统计指定时间之后的扣费总额（SQL 聚合，避免把明细行全部查回内存） */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM t_payment_record WHERE created_at >= #{start}")
    BigDecimal sumAmountSince(@Param("start") LocalDateTime start);

    /** 按自然日统计扣费金额（首页区间柱状图，SQL 聚合） */
    @Select("SELECT DATE(created_at) AS d, COALESCE(SUM(amount), 0) AS v FROM t_payment_record "
            + "WHERE created_at >= #{start} AND created_at < #{end} GROUP BY DATE(created_at)")
    List<Map<String, Object>> sumAmountByDay(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
