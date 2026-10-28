package com.parking.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parking.system.domain.ParkingZone;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ParkingZoneMapper extends BaseMapper<ParkingZone> {
}
