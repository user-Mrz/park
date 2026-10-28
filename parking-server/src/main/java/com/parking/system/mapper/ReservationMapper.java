package com.parking.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parking.system.domain.Reservation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReservationMapper extends BaseMapper<Reservation> {
}
