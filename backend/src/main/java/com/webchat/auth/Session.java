package com.webchat.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sessions")
public class Session {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "refresh_hash", nullable = false)
    private String refreshHash;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "ip")
    private String ip;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected Session() {}

    public Session(UUID id, Long userId, String refreshHash, String userAgent, String ip, Instant expiresAt) {
        this.id = id;
        this.userId = userId;
        this.refreshHash = refreshHash;
        this.userAgent = userAgent;
        this.ip = ip;
        Instant now = Instant.now();
        this.createdAt = now;
        this.lastSeenAt = now;
        this.expiresAt = expiresAt;
    }

    public UUID getId() { return id; }
    public Long getUserId() { return userId; }
    public String getRefreshHash() { return refreshHash; }
    public void setRefreshHash(String refreshHash) { this.refreshHash = refreshHash; }
    public String getUserAgent() { return userAgent; }
    public String getIp() { return ip; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }

    public boolean isActive() {
        return revokedAt == null && expiresAt.isAfter(Instant.now());
    }
}
