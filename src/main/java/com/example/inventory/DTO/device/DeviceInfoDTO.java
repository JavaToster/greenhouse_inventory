package com.example.inventory.DTO.device;

import com.example.inventory.util.enums.DeviceStatus;
import lombok.Data;

import java.util.UUID;

@Data
public class DeviceInfoDTO {
    private UUID id;
    private DeviceStatus deviceStatus;
}
