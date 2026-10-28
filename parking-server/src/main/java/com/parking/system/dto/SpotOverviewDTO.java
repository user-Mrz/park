package com.parking.system.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SpotOverviewDTO {

    /** All parking spots grouped by area */
    private List<AreaGroup> areaGroups;

    /** All currently parked vehicles (status = 0) */
    private List<ParkedVehicle> parkedVehicles;

    /** All active reservations (status = 1) */
    private List<ActiveReservation> activeReservations;

    @Data
    public static class AreaGroup {
        private String area;
        private List<SpotInfo> spots;
    }

    @Data
    public static class SpotInfo {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String spotCode;
        private String area;
        /** 校园分区 code（teaching/library/dorm/gym/visitor/tidal），用于按角色可见区域过滤 */
        private String zoneCode;
        private String floor;
        private BigDecimal pricePerHour;
        private Integer status;
        /** 允许停放的身份串（"1,2,3"，空表示不限）；不对外输出，仅用于按身份过滤 */
        @JsonIgnore
        private String allowIdentities;
    }

    @Data
    public static class ParkedVehicle {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long recordId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long spotId;
        private String spotCode;
        private String plateNumber;
        private String nickname;
        private String phone;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
        private LocalDateTime entryTime;
        private BigDecimal spotPricePerHour;
    }

    @Data
    public static class ActiveReservation {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long userId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long spotId;
        private String reservationNo;
        private String nickname;
        private String phone;
        private String plateNumber;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
        private LocalDateTime startTime;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
        private LocalDateTime endTime;
        private BigDecimal durationHours;
        private BigDecimal totalAmount;
        private Integer status;
    }
}
