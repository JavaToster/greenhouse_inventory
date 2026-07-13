package com.example.inventory.DTO.device;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Collection of temporary device secrets for a cluster")
public record DevicesSecretWrapper(

        @Schema(
                description = "Unique cluster identifier",
                example = "550e8400-e29b-41d4-a716-446655440000"
        )
        UUID clusterId,

        @Schema(
                description = "Temporary secrets assigned to cluster devices"
        )
        List<ClusterDevicesTempSecretsDTO> secrets

) {
}