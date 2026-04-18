package com.webchat.auth.dto;

import com.webchat.auth.Session;
import java.time.Instant;
import java.util.UUID;

public record SessionResponse(
        UUID id,
        String userAgent,
        String ip,
        Instant createdAt,
        Instant lastSeenAt,
        Instant expiresAt,
        boolean current
) {
    public static SessionResponse from(Session s, boolean current) {
        return new SessionResponse(
                s.getId(),
                s.getUserAgent(),
                s.getIp(),
                s.getCreatedAt(),
                s.getLastSeenAt(),
                s.getExpiresAt(),
                current
        );
    }
}
