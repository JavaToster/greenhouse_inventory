package com.example.inventory.DTO.cluster;

import com.example.inventory.DTO.device.DeviceInfoDTO;
import lombok.Data;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
public class ClusterInfoDTO {
    private UUID id;
    private String name;
    private String description;
    private Long ownerId;
    private List<DeviceInfoDTO> devices;
    private Set<Long> workerIds;
}
