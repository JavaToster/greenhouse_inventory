package com.example.inventory.controllers;

import com.example.inventory.DTO.device.ClusterDevicesTempSecretsDTO;
import com.example.inventory.services.ClusterService;
import com.example.inventory.services.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/devices")
public class DeviceController {
    private final DeviceService deviceService;

    @DeleteMapping("/{id}/remove")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UUID> removeDevice(@PathVariable("id") UUID deviceId){
        return ResponseEntity.ok(deviceService.remove(deviceId));
    }

    @GetMapping("/secrets/{token}")
    @PreAuthorize("hasAnyRole('INSTALLER', 'ADMIN')")
    public ResponseEntity<List<ClusterDevicesTempSecretsDTO>> getSecrets(@PathVariable("token") UUID token){
        List<ClusterDevicesTempSecretsDTO> secrets = deviceService.getRawKeysAndActivate(token);
        return ResponseEntity.ok(secrets);
    }
}
