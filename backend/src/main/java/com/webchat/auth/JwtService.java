package com.webchat.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final Duration ACCESS_TTL = Duration.ofMinutes(15);

    private final SecretKey key;

    public JwtService(@Value("${webchat.jwt.secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("webchat.jwt.secret must be at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    public String issueAccessToken(Long userId, UUID sessionId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("sid", sessionId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ACCESS_TTL)))
                .signWith(key)
                .compact();
    }

    public Parsed parse(String token) {
        Claims c = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new Parsed(Long.valueOf(c.getSubject()), UUID.fromString(c.get("sid", String.class)));
    }

    public Duration accessTtl() { return ACCESS_TTL; }

    public record Parsed(Long userId, UUID sessionId) {}
}
