package com.example.inventory.DTO.error;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;

@Schema(description = "Standard error response")
public record ErrorResponseDTO(
        @Schema(description = "HTTP status code", example = "400")
        int statusCode,
        @Schema(description = "Human-readable error message", example = "Validation error")
        String message,
        @Schema(description = "Unix timestamp in milliseconds", example = "1710000000000")
        long timestamp
) {
    public ErrorResponseDTO(int statusCode, String message) {
        this(statusCode, message, new Date().getTime());
    }
}