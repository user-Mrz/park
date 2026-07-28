package com.parking.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parking.dto.SpotOverviewDTO;
import com.parking.entity.ParkingSpot;

public interface SpotService {
    Page<ParkingSpot> getAvailableSpots(int page, int pageSize, String area);
    SpotOverviewDTO getOverview();
}
