package com.example.inventory.DTO.device;

import java.util.List;
import java.util.UUID;

public record DevicesSecretWrapper(
        UUID clusterId,
        List<ClusterDevicesTempSecretsDTO> secrets
) {}
