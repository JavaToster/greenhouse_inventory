package com.example.inventory.DTO.user;

import com.example.inventory.util.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Information about a system user")
public record UserInfoDTO(

        @Schema(
                description = "Telegram user identifier",
                example = "123456789"
        )
        long telegramId,

        @Schema(
                description = "User email address",
                example = "john.doe@example.com"
        )
        String email,

        @Schema(
                description = "User role",
                example = "OWNER"
        )
        Role role
) {}