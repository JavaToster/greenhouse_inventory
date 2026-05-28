package com.example.inventory.DTO.cluster;

import com.example.inventory.DTO.device.DeviceInfoDTO;
import com.example.inventory.DTO.user.UserInfoDTO;

import java.util.List;
import java.util.UUID;

public record ClustersWithUserDetailsDTO(
        UUID id,
        String name,
        String description,
        UserInfoDTO owner,
        List<UserInfoDTO> workers,
        List<DeviceInfoDTO> devices
) {}
