package com.example.inventory.DTO.device;

import com.example.inventory.util.enums.DeviceStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Basic information about a device")
public record DeviceInfoDTO(

        @Schema(
                description = "Unique device identifier",
                example = "550e8400-e29b-41d4-a716-446655440000"
        )
        UUID id,

        @Schema(
                description = "Current device status",
                example = "ACTIVE"
        )
        DeviceStatus deviceStatus

) {
}