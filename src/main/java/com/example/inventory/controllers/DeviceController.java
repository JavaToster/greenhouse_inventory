package com.example.inventory.controllers;

import com.example.inventory.DTO.device.ClusterDevicesTempSecretsDTO;
import com.example.inventory.DTO.device.DeviceInfoDTO;
import com.example.inventory.security.principals.UserPrincipal;
import com.example.inventory.services.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/devices")
@Tag(name = "Devices", description = "Endpoints for managing greenhouse devices")
@SecurityRequirement(name = "bearerAuth")
public class DeviceController {
    private final DeviceService deviceService;

    @DeleteMapping("/{id}/remove")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove device")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Device removed successfully")
    })
    public ResponseEntity<UUID> removeDevice(@PathVariable("id") UUID deviceId){
        log.info("Received request to remove device id={}", deviceId);
        return ResponseEntity.ok(deviceService.remove(deviceId));
    }

    @GetMapping("/secrets/{token}")
    @PreAuthorize("hasAnyRole('INSTALLER', 'ADMIN')")
    @Operation(summary = "Get raw device secrets and activate devices")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Device secrets returned successfully")
    })
    public ResponseEntity<List<ClusterDevicesTempSecretsDTO>> getSecrets(@PathVariable("token") UUID token){
        log.info("Received request to fetch device secrets using activation token='{}'", token);
        List<ClusterDevicesTempSecretsDTO> secrets = deviceService.getRawKeysAndActivate(token);
        return ResponseEntity.ok(secrets);
    }

    @GetMapping("/my-clusters/{clusterId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN') and principal instanceof T(com.example.inventory.security.principals.UserPrincipal)")
    @Operation(summary = "Get devices by cluster")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Devices returned successfully")
    })
    public ResponseEntity<List<DeviceInfoDTO>> getDevicesByCluster(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable("clusterId") UUID clusterId) {
        log.debug("Received request from user telegramId={} to fetch devices for cluster id={}",
                userPrincipal.telegramId(), clusterId);
        List<DeviceInfoDTO> devices = deviceService.findByClusterAndCheckOwner(clusterId, userPrincipal.telegramId());
        return ResponseEntity.ok(devices);
    }
}