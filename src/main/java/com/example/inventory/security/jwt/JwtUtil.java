package com.example.inventory.security.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.inventory.util.enums.TokenType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;
import com.example.inventory.util.enums.Role;

@Slf4j
@Component
public class JwtUtil {
    private static final String ISSUER = "greenhouse";
    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String ROLE_CLAIM = "role";

    private final String secret;

    public JwtUtil(@Value("${spring.security.jwt.secret}") String secret){
        this.secret = secret;
    }

    public String generateToken(String subject, TokenType tokenType, Map<String, Object> claims, Duration ttl) {
        log.debug("Inventory: generating JWT token for subject={}, tokenType={}, ttl={}", subject, tokenType, ttl);
        Date issuedAt = new Date();
        Date expirationDate = Date.from(ZonedDateTime.now().plus(ttl).toInstant());

        var jwtBuilder = JWT.create()
                .withSubject(subject)
                .withIssuedAt(issuedAt)
                .withIssuer(ISSUER)
                .withClaim(TOKEN_TYPE_CLAIM, tokenType.name())
                .withExpiresAt(expirationDate);

        if (claims != null) {
            claims.forEach((key, value) -> appendClaim(jwtBuilder, key, value));
        }

        String token = jwtBuilder.sign(Algorithm.HMAC256(secret));
        log.debug("Inventory: JWT token successfully generated for subject={}, expires at={}", subject, expirationDate);
        return token;
    }

    public DecodedJWT verify(String token) {
        log.debug("Inventory: verifying JWT token signature and issuer");
        JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secret))
                .withIssuer(ISSUER)
                .build();

        return verifier.verify(token);
    }

    public TokenType getTokenType(DecodedJWT jwt) {
        String type = jwt.getClaim(TOKEN_TYPE_CLAIM).asString();
        if (type == null || type.isBlank()) {
            log.warn("Inventory: JWT is missing the required '{}' claim", TOKEN_TYPE_CLAIM);
            throw new IllegalArgumentException("Missing token_type claim");
        }
        return TokenType.valueOf(type);
    }

    private void appendClaim(com.auth0.jwt.JWTCreator.Builder jwtBuilder, String key, Object value) {
        switch (value) {
            case String stringValue -> jwtBuilder.withClaim(key, stringValue);
            case Integer intValue -> jwtBuilder.withClaim(key, intValue);
            case Long longValue -> jwtBuilder.withClaim(key, longValue);
            case Double doubleValue -> jwtBuilder.withClaim(key, doubleValue);
            case Boolean booleanValue -> jwtBuilder.withClaim(key, booleanValue);
            case null, default -> throw new IllegalArgumentException("Unsupported JWT claim type for key: " + key);
        }
    }

    public Role getRole(DecodedJWT jwt) {
        String role = jwt.getClaim(ROLE_CLAIM).asString();
        return (role != null) ? Role.valueOf(role) : Role.ROLE_UNKNOWN;
    }
}