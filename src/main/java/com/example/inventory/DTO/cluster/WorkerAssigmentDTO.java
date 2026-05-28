package com.example.inventory.DTO.cluster;

import jakarta.validation.constraints.Min;

public record WorkerAssigmentDTO(
        @Min(value = 1, message = "Id СЂР°Р±РѕС‚РЅРёРєР° РґРѕР»Р¶РЅРѕ Р±С‹С‚СЊ Р·Р°РїРѕР»РЅРµРЅРѕ")
        long workerId
) {}
