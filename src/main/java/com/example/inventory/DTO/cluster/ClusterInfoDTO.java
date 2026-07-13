package com.example.inventory.DTO.cluster;

import com.example.inventory.DTO.device.DeviceInfoDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Schema(description = "Detailed information about a greenhouse cluster")
public record ClusterInfoDTO(

        @Schema(
                description = "Unique cluster identifier",
                example = "550e8400-e29b-41d4-a716-446655440000"
        )
        UUID id,

        @Schema(
                description = "Cluster name",
                example = "Greenhouse Cluster A"
        )
        String name,

        @Schema(
                description = "Cluster description",
                example = "Main greenhouse used for tomato cultivation"
        )
        String description,

        @Schema(
                description = "Telegram ID of the cluster owner",
                example = "123456789"
        )
        Long ownerId,

        @Schema(description = "Devices assigned to the cluster")
        List<DeviceInfoDTO> devices,

        @Schema(
                description = "Telegram IDs of workers assigned to the cluster"
        )
        Set<Long> workerIds

) {
}