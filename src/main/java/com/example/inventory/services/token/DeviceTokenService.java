package com.example.inventory.services.token;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.inventory.security.JwtUtil;
import com.example.inventory.security.jwt.TokenType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeviceTokenService {
    private static final String CLUSTER_ID_CLAIM = "cluster_id";
    private static final Duration DEVICE_TOKEN_TTL = Duration.ofMinutes(30);

    private final JwtUtil jwtUtil;

    public String generate(UUID deviceId, UUID clusterId) {
        return jwtUtil.generateToken(
                deviceId.toString(),
                TokenType.DEVICE,
                Map.of(CLUSTER_ID_CLAIM, clusterId.toString()),
                DEVICE_TOKEN_TTL
        );
    }

    public UUID validateAndGetDeviceId(String token) {
        DecodedJWT jwt = jwtUtil.verify(token);
        TokenType tokenType = jwtUtil.getTokenType(jwt);
        if (tokenType != TokenType.DEVICE) {
            throw new IllegalArgumentException("Token is not a device token");
        }

        return UUID.fromString(jwt.getSubject());
    }

    public UUID getClusterId(String token) {
        DecodedJWT jwt = jwtUtil.verify(token);
        return UUID.fromString(jwt.getClaim(CLUSTER_ID_CLAIM).asString());
    }
}
