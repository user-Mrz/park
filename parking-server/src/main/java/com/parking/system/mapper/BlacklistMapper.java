package com.parking.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parking.system.domain.Blacklist;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BlacklistMapper extends BaseMapper<Blacklist> {
}
