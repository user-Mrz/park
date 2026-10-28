package com.parking.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parking.system.domain.ScheduledJob;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ScheduledJobMapper extends BaseMapper<ScheduledJob> {
}
