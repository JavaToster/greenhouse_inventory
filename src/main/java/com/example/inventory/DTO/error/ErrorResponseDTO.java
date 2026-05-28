package com.example.inventory.DTO.error;

import java.util.Date;

public record ErrorResponseDTO(
        int statusCode,
        String message,
        long timestamp
) {
    public ErrorResponseDTO(int statusCode, String message) {
        this(statusCode, message, new Date().getTime());
    }
}
