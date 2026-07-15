package com.example.inventory.security.jwt;

import com.example.inventory.util.enums.TokenType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceJwtTokenIssuer {
    private static final String CLUSTER_ID_CLAIM = "cluster_id";
    private static final Duration DEVICE_TOKEN_TTL = Duration.ofMinutes(30);

    private final JwtUtil jwtUtil;

    public String generate(UUID deviceId, UUID clusterId) {
        log.info("Generating new JWT token for device id={} (assigned to cluster id={})", deviceId, clusterId);

        String token = jwtUtil.generateToken(
                deviceId.toString(),
                TokenType.DEVICE,
                Map.of(CLUSTER_ID_CLAIM, clusterId.toString()),
                DEVICE_TOKEN_TTL
        );

        log.debug("Device token successfully created with TTL={} min", DEVICE_TOKEN_TTL.toMinutes());
        return token;
    }
}