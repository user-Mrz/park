package com.parking.system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VehicleEnterDTO {
    @NotNull(message = "车位 ID 不能为空")
    private Long spotId;

    private Long reservationId;

    private String plateNumber;
}
