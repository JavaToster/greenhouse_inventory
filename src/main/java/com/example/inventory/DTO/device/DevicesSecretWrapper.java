package com.example.inventory.DTO.device;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DevicesSecretWrapper {
    private UUID clusterId;
    private List<ClusterDevicesTempSecretsDTO> secrets;
}
