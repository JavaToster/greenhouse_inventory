package com.example.inventory.DTO.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Request used by a device to complete challenge-response authentication")
public record DeviceAuthRequestDTO(

        @NotNull(message = "Device ID is required")
        @Schema(
                description = "Unique device identifier",
                example = "550e8400-e29b-41d4-a716-446655440000"
        )
        UUID deviceId,

        @NotBlank(message = "Challenge cannot be blank")
        @Schema(
                description = "Challenge previously issued by the server",
                example = "a8f5f167f44f4964e6c998dee827110c"
        )
        String challenge,

        @NotBlank(message = "Signature cannot be blank")
        @Schema(
                description = "Digital signature of the challenge generated using the device secret",
                example = "MEUCIQCxJz5Y8eV6y7J6R3K8h4Q4f8b7r9v2Y5r3W6s8kAIgM7zP4X1f2W9a8b6c5d4e3f2g1h0i9j8k7l6m5n4o3p2"
        )
        String signature

) {
}