package com.example.inventory.DTO.cluster;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

@Schema(description = "Request to assign a worker to a cluster")
public record WorkerAssignmentDTO(

        @Schema(
                description = "Telegram ID of the worker",
                example = "987654321"
        )
        @Min(value = 1, message = "Worker ID must be provided and greater than 0")
        long workerId

) {
}