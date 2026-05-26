package com.example.inventory.security;

import com.example.inventory.util.enums.Role;

public record UserPrincipal(long telegramId, Role role){
}