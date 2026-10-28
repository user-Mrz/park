package com.parking.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parking.system.domain.AdminOperationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminOperationLogMapper extends BaseMapper<AdminOperationLog> {
}
