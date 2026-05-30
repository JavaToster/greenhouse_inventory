package com.example.inventory.security.jwt;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.inventory.exceptions.auth.InvalidTokenTypeException;
import com.example.inventory.security.principals.DevicePrincipal;
import com.example.inventory.security.principals.UserPrincipal;
import com.example.inventory.util.enums.TokenType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import com.example.inventory.util.enums.Role;

@Service
@RequiredArgsConstructor
public class JwtAuthenticationProvider {
    private static final String CLUSTER_ID_CLAIM = "cluster_id";

    private final JwtUtil jwtUtil;

    public Authentication authenticate(String token) {
        DecodedJWT jwt = jwtUtil.verify(token);
        TokenType tokenType;
        try {
            tokenType = jwtUtil.getTokenType(jwt);
        } catch (RuntimeException ex) {
            throw new InvalidTokenTypeException("Invalid token_type claim", ex);
        }

        return switch (tokenType) {
            case USER -> authenticateUser(jwt, token);
            case DEVICE -> authenticateDevice(jwt, token);
        };
    }

    private Authentication authenticateUser(DecodedJWT jwt, String token) {
        long telegramId = Long.parseLong(jwt.getSubject());
        Role role = jwtUtil.getRole(jwt);
        UserPrincipal userPrincipal = new UserPrincipal(telegramId, role);

        return new UsernamePasswordAuthenticationToken(
                userPrincipal,
                token,
                List.of(new SimpleGrantedAuthority(userPrincipal.role().name()))
                );
    }

    private Authentication authenticateDevice(DecodedJWT jwt, String token) {
        UUID deviceId = UUID.fromString(jwt.getSubject());
        String clusterIdRaw = jwt.getClaim(CLUSTER_ID_CLAIM).asString();
        if (clusterIdRaw == null || clusterIdRaw.isBlank()) {
            throw new IllegalArgumentException("Device token has no cluster_id claim");
        }

        DevicePrincipal principal = new DevicePrincipal(deviceId, UUID.fromString(clusterIdRaw));
        return new UsernamePasswordAuthenticationToken(
                principal,
                token,
                List.of(new SimpleGrantedAuthority("ROLE_DEVICE"))
        );
    }
}
