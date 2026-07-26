package com.parking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VehicleEnterDTO {
    @NotNull(message = "Spot ID is required")
    private Long spotId;

    private Long reservationId;

    private String plateNumber;
}
