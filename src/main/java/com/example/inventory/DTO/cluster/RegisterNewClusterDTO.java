package com.example.inventory.DTO.cluster;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to register a new greenhouse cluster")
public record RegisterNewClusterDTO(

        @Schema(
                description = "Telegram ID of the cluster owner",
                example = "123456789"
        )
        @NotNull(message = "Cluster owner Telegram ID is required")
        Long ownerId,

        @Schema(
                description = "Number of devices to create for the cluster",
                example = "10"
        )
        @Min(value = 1, message = "Device count must be between 1 and 100")
        @Max(value = 100, message = "Device count must be between 1 and 100")
        int devicesCount,

        @Schema(
                description = "Human-readable cluster name",
                example = "Greenhouse Cluster A"
        )
        @NotNull(message = "Cluster name cannot be empty")
        String name

) {
}