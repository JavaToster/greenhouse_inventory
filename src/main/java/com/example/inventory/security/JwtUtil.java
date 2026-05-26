package com.example.inventory.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.inventory.security.jwt.TokenType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import com.example.inventory.util.enums.Role;

@Component
public class JwtUtil {
    private static final String ISSUER = "greenhouse";
    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String ROLE_CLAIM = "role";


    @Value("${spring.security.jwt.secret}")
    private String secret;

    // public String generateToken(long telegramId){
    //     return generateToken(
    //             String.valueOf(telegramId),
    //             TokenType.USER,
    //             Collections.emptyMap(),
    //             Duration.ofDays(365)
    //     );
    // }

    public String generateToken(String subject, TokenType tokenType, Map<String, Object> claims, Duration ttl) {
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

        return jwtBuilder.sign(Algorithm.HMAC256(secret));
    }

    public DecodedJWT verify(String token) {
        JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secret))
                .withIssuer(ISSUER)
                .build();

        return verifier.verify(token);
    }

    public TokenType getTokenType(DecodedJWT jwt) {
        String type = jwt.getClaim(TOKEN_TYPE_CLAIM).asString();
        if (type == null || type.isBlank()) {
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
