package com.parking.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parking.system.domain.MonthlyPass;
import org.apache.ibatis.annotations.Mapper;

/**
 * 月卡 Mapper。
 */
@Mapper
public interface MonthlyPassMapper extends BaseMapper<MonthlyPass> {
}
