package com.example.inventory.DTO.device;

import com.example.inventory.util.enums.DeviceStatus;

import java.util.UUID;

public record DeviceInfoDTO(
        UUID id,
        DeviceStatus deviceStatus
) {}
