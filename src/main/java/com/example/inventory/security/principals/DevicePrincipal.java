package com.example.inventory.security.principals;

import java.util.UUID;

public record DevicePrincipal(UUID deviceId, UUID clusterId) {
}
