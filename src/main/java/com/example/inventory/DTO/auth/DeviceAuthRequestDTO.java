package com.example.inventory.DTO.auth;

import java.util.UUID;

public record DeviceAuthRequestDTO(
        UUID deviceId,
        String challenge,
        String signature
){}
