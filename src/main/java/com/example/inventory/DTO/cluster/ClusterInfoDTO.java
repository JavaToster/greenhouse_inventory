package com.example.inventory.DTO.cluster;

import com.example.inventory.DTO.device.DeviceInfoDTO;

import java.util.List;
import java.util.Set;
import java.util.UUID;
public record ClusterInfoDTO(UUID id, String name, String description, Long ownerId, List<DeviceInfoDTO> devices, Set<Long> workerIds){}
