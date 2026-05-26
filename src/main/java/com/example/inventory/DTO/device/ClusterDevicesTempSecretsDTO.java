package com.example.inventory.DTO.device;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class ClusterDevicesTempSecretsDTO {
    private UUID id;
    private String rawSecret;
}
