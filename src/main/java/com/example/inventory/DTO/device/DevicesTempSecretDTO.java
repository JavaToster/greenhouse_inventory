package com.example.inventory.DTO.device;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Temporary secret assigned to a cluster")
public record DevicesTempSecretDTO(

        @Schema(
                description = "Unique cluster identifier",
                example = "550e8400-e29b-41d4-a716-446655440000"
        )
        UUID clusterId,

        @Schema(
                description = "Temporary secret used for device provisioning",
                example = "c8f91d4a7e534e3b9f2d1c6a5b8e7f90"
        )
        String token

) {
}