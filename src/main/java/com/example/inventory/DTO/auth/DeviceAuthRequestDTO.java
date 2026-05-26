package com.example.inventory.DTO.auth;

import lombok.Data;

import java.util.UUID;

@Data
public class DeviceAuthRequestDTO {
    private UUID deviceId;
    private String challenge;
    private String signature;
}
