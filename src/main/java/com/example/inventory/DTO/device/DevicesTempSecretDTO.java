package com.example.inventory.DTO.device;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DevicesTempSecretDTO {
    private String clusterId;
    private String token;
}
