package com.example.inventory.DTO.device;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Temporary authentication secret assigned to a device")
public record ClusterDevicesTempSecretsDTO(

        @Schema(
                description = "Unique device identifier",
                example = "550e8400-e29b-41d4-a716-446655440000"
        )
        UUID id,

        @Schema(
                description = "Temporary secret used during the initial device provisioning",
                example = "c8f91d4a7e534e3b9f2d1c6a5b8e7f90"
        )
        String rawSecret

) {
}