package com.example.inventory.DTO.device;

import java.util.UUID;

public record ClusterDevicesTempSecretsDTO(
        UUID id,
        String rawSecret
) {}
