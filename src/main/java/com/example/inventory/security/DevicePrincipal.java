package com.example.inventory.security;

import java.util.UUID;

public record DevicePrincipal(UUID deviceId, UUID clusterId) {
}
