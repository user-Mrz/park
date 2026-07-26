package com.parking.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parking.entity.ParkingSpot;
import com.parking.mapper.ParkingSpotMapper;
import com.parking.service.SpotService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SpotServiceImpl implements SpotService {

    private final ParkingSpotMapper parkingSpotMapper;

    @Override
    public Page<ParkingSpot> getAvailableSpots(int page, int pageSize, String area) {
        LambdaQueryWrapper<ParkingSpot> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(area)) {
            wrapper.eq(ParkingSpot::getArea, area);
        }
        wrapper.ne(ParkingSpot::getStatus, 3); // exclude maintenance
        wrapper.orderByAsc(ParkingSpot::getSpotCode);
        return parkingSpotMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }
}
