package com.example.inventory.DTO.cluster;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class WorkerAssigmentDTO {
    @Min(value = 1, message = "Id работника должно быть заполнено")
    private long workerId;
}
