package com.example.inventory.DTO.cluster;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RegisterNewClusterDTO(
        @NotNull(message = "РћР±СЏР·Р°С‚РµР»СЊРЅРѕ РІРІРµРґРёС‚Рµ telegram id С…РѕР·СЏРёРЅР° РєР»Р°СЃС‚РµСЂР°")
        Long ownerId,
        @Min(value = 1, message = "РљРѕР»РёС‡РµСЃС‚РІРѕ РґРµРІР°Р№СЃРѕРІ РґРѕР»Р¶РЅРѕ Р±С‹С‚СЊ РјРёРЅРёРјСѓРј 1 РјР°РєСЃРёРјСѓРј 100")
        @Max(value = 100, message = "РљРѕР»РёС‡РµСЃС‚РІРѕ РґРµРІР°Р№СЃРѕРІ РґРѕР»Р¶РЅРѕ Р±С‹С‚СЊ РјРёРЅРёРјСѓРј 1 РјР°РєСЃРёРјСѓРј 100")
        int devicesCount,
        @NotNull(message = "РРјСЏ РЅРµ РґРѕР»Р¶РЅРѕ Р±С‹С‚СЊ РїСѓСЃС‚С‹Рј")
        String name
) {}
