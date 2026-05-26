package com.example.inventory.DTO.cluster;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegisterNewClusterDTO {
    @NotNull(message = "Обязательно введите telegram id хозяина кластера")
    private long ownerId;
    @Min(value=1, message = "Количество девайсов должно быть минимум 1 максимум 100")
    @Max(value=100, message = "Количество девайсов должно быть минимум 1 максимум 100")
    private int devicesCount;
    @NotNull(message = "Имя не должно быть пустым")
    private String name;
}
