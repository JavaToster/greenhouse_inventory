package com.example.inventory.DTO.user;

import com.example.inventory.util.enums.Role;

public record UserInfoDTO(
        long telegramId,
        String email,
        Role role
) {}
