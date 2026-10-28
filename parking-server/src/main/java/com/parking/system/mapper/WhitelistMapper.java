package com.parking.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parking.system.domain.Whitelist;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WhitelistMapper extends BaseMapper<Whitelist> {
}
