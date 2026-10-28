package com.parking.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parking.system.domain.Violation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ViolationMapper extends BaseMapper<Violation> {
}
