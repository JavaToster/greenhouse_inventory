package com.example.inventory.DTO.user;

import com.example.inventory.util.enums.Role;
import lombok.Data;

@Data
public class UserInfoDTO {
    private long telegramId;
    private String email;
    private Role role;
}
