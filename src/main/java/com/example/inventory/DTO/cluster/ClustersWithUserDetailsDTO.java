package com.example.inventory.DTO.cluster;

import com.example.inventory.DTO.device.DeviceInfoDTO;
import com.example.inventory.DTO.user.UserInfoDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Detailed information about the greenhouse cluster including owner, workers, and connected devices")
public record ClustersWithUserDetailsDTO(
        @Schema(description = "Unique identifier of the cluster", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID id,

        @Schema(description = "Name of the greenhouse cluster", example = "Kazan Main Cluster No. 1")
        String name,

        @Schema(description = "Detailed description of the cluster and its location", example = "Main production block, localized sector for tomato cultivation")
        String description,

        @Schema(description = "Information about the cluster owner")
        UserInfoDTO owner,

        @Schema(description = "List of workers assigned to this cluster")
        List<UserInfoDTO> workers,

        @Schema(description = "List of devices installed within the cluster")
        List<DeviceInfoDTO> devices
) {}