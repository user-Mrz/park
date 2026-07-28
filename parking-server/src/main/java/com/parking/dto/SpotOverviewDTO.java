package com.parking.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
        private Long id;
        private String spotCode;
        private String area;
        private String floor;
        private BigDecimal pricePerHour;
        private Integer status;
    }

    @Data
    public static class ParkedVehicle {
        private Long recordId;
        private Long spotId;
        private String spotCode;
        private String plateNumber;
        private String nickname;
        private String phone;
        private LocalDateTime entryTime;
        private BigDecimal spotPricePerHour;
    }

    @Data
    public static class ActiveReservation {
        private Long id;
        private Long userId;
        private Long spotId;
        private String reservationNo;
        private String nickname;
        private String phone;
        private String plateNumber;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private BigDecimal durationHours;
        private BigDecimal totalAmount;
        private Integer status;
    }
}
