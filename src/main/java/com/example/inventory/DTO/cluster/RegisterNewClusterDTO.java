package com.example.inventory.DTO.cluster;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RegisterNewClusterDTO(
        @NotNull(message = "Cluster owner Telegram ID is required")
        Long ownerId,
        @Min(value = 1, message = "Device count must be between 1 and 100")
        @Max(value = 100, message = "Device count must be between 1 and 100")
        int devicesCount,
        @NotNull(message = "Cluster name cannot be empty")
        String name
) {}