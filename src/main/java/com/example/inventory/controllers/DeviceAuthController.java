package com.example.inventory.controllers;

import com.example.inventory.DTO.auth.ChallengeDTO;
import com.example.inventory.DTO.auth.DeviceAuthRequestDTO;
import com.example.inventory.DTO.auth.SuccessfullyAuthenticatedDTO;
import com.example.inventory.services.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/devices/auth")
@Tag(name = "Device Authentication", description = "Endpoints for device challenge-response authentication")
public class DeviceAuthController {

    private final DeviceService deviceService;

    @PostMapping("/challenge/{deviceId}")
    @Operation(summary = "Get authentication challenge for a device")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Challenge generated successfully")
    })
    @SecurityRequirements
    public ResponseEntity<ChallengeDTO> getChallenge(@PathVariable UUID deviceId) throws BadRequestException {
        log.debug("Received request to generate authentication challenge for device id={}", deviceId);
        String challenge = deviceService.generateChallenge(deviceId);
        return ResponseEntity.ok(new ChallengeDTO(challenge));
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify device challenge response and issue a token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Device authenticated successfully")
    })
    @SecurityRequirements
    public ResponseEntity<SuccessfullyAuthenticatedDTO> verify(@RequestBody DeviceAuthRequestDTO deviceAuthRequestDTO) {
        log.debug("Received request to verify authentication challenge for device id={}", deviceAuthRequestDTO.deviceId());
        String token = deviceService.verify(deviceAuthRequestDTO);
        return ResponseEntity.ok(new SuccessfullyAuthenticatedDTO(token));
    }
}