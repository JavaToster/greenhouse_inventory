package com.example.inventory.security.jwt;

import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceTokenProvider {
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

    public UUID validateAndGetDeviceId(String token) {
        log.debug("Validating device token");
        DecodedJWT jwt = jwtUtil.verify(token);
        TokenType tokenType = jwtUtil.getTokenType(jwt);

        if (tokenType != TokenType.DEVICE) {
            log.warn("Token validation failed: expected token type {}, but received {}", TokenType.DEVICE, tokenType);
            throw new IllegalArgumentException("Token is not a device token");
        }

        String deviceIdStr = jwt.getSubject();
        log.debug("Device token is valid. Found device id={}", deviceIdStr);
        return UUID.fromString(deviceIdStr);
    }

    public UUID getClusterId(String token) {
        log.debug("Extracting cluster claim from device token");
        DecodedJWT jwt = jwtUtil.verify(token);
        String clusterIdStr = jwt.getClaim(CLUSTER_ID_CLAIM).asString();

        log.debug("Extracted cluster id={} from token claim", clusterIdStr);
        return UUID.fromString(clusterIdStr);
    }
}