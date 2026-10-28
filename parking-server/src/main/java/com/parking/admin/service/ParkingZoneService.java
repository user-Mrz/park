package com.parking.admin.service;

import com.parking.system.dto.ParkingZoneDTO;
import com.parking.system.domain.ParkingZone;

import java.util.List;

/**
 * 校园停车区域管理。
 */
public interface ParkingZoneService {

    /** 管理端列表：含各区域车位数；status 为 null 时返回全部 */
    List<ParkingZone> listAll(Integer status);

    /** 用户端可见区域：仅启用状态，按展示顺序排列 */
    List<ParkingZone> listEnabled();

    ParkingZone create(ParkingZoneDTO dto);

    /** 修改名称 / 说明 / 排序（区域编码不可改） */
    ParkingZone update(Long id, ParkingZoneDTO dto);

    /** 启停区域：停用前要求该区域下没有车位，避免车位变成"无主" */
    void updateStatus(Long id, Integer status);

    /** 删除区域：仅当该区域下没有车位时允许 */
    void delete(Long id);
}
