package com.parking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parking.entity.Violation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ViolationMapper extends BaseMapper<Violation> {
}
