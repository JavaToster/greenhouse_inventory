package com.example.inventory.DTO.error;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
public class ErrorResponseDTO {
    private int statusCode;
    private String message;
    private long timestamp;

    public ErrorResponseDTO(int statusCode, String message){
        this.statusCode = statusCode;
        this.message = message;
        this.timestamp = new Date().getTime();
    }
}
