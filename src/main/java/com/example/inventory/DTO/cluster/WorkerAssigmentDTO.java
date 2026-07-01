package com.example.inventory.DTO.cluster;

import jakarta.validation.constraints.Min;

public record WorkerAssigmentDTO(
        @Min(value = 1, message = "Worker ID must be provided and greater than 0")
        long workerId
) {}