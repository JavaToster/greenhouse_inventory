package com.example.inventory.controllers;

import com.example.inventory.DTO.auth.ChallengeDTO;
import com.example.inventory.DTO.auth.DeviceAuthRequestDTO;
import com.example.inventory.DTO.auth.SuccessfullyAuthenticatedDTO;
import com.example.inventory.services.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/devices/auth")
public class DeviceAuthController {

    private final DeviceService deviceService;

    @PostMapping("/challenge/{deviceId}")
    public ResponseEntity<ChallengeDTO> getChallenge(@PathVariable String deviceId){
        String challenge = deviceService.generateChallenge(deviceId);
        return ResponseEntity.ok(new ChallengeDTO(challenge));
    }

    @PostMapping("/verify")
    public ResponseEntity<SuccessfullyAuthenticatedDTO> verify(@RequestBody DeviceAuthRequestDTO deviceAuthRequestDTO) {
        String token = deviceService.verify(deviceAuthRequestDTO);
        return ResponseEntity.ok(new SuccessfullyAuthenticatedDTO(token));
    }
}
