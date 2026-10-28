package com.parking.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parking.system.domain.ParkingSpot;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ParkingSpotMapper extends BaseMapper<ParkingSpot> {
}
