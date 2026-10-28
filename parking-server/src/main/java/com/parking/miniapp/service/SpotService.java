package com.parking.miniapp.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parking.system.dto.SpotOverviewDTO;
import com.parking.system.domain.ParkingSpot;

public interface SpotService {
    Page<ParkingSpot> getAvailableSpots(int page, int pageSize, String area);
    SpotOverviewDTO getOverview();
}
