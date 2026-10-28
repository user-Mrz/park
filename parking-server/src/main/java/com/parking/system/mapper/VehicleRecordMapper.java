package com.parking.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parking.system.domain.VehicleRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface VehicleRecordMapper extends BaseMapper<VehicleRecord> {

    /** 按自然日统计入场车次（首页区间柱状图，SQL 聚合） */
    @Select("SELECT DATE(entry_time) AS d, COUNT(*) AS v FROM t_vehicle_record "
            + "WHERE entry_time >= #{start} AND entry_time < #{end} GROUP BY DATE(entry_time)")
    List<Map<String, Object>> countByDay(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
