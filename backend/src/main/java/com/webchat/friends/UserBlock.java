package com.webchat.friends;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "user_blocks")
public class UserBlock {

    @EmbeddedId
    private UserBlockId id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserBlock() {}

    public UserBlock(Long blockerId, Long blockedId) {
        this.id = new UserBlockId(blockerId, blockedId);
        this.createdAt = Instant.now();
    }

    public UserBlockId getId() { return id; }
    public Long getBlockerId() { return id.getBlockerId(); }
    public Long getBlockedId() { return id.getBlockedId(); }
    public Instant getCreatedAt() { return createdAt; }
}
