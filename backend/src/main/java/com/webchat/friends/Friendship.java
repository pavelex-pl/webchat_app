package com.webchat.friends;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "friendships")
public class Friendship {

    @EmbeddedId
    private FriendshipId id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FriendshipStatus status;

    @Column(name = "initiated_by", nullable = false)
    private Long initiatedBy;

    @Column(name = "request_text")
    private String requestText;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    protected Friendship() {}

    public Friendship(Long from, Long to, String requestText) {
        this.id = FriendshipId.canonical(from, to);
        this.status = FriendshipStatus.PENDING;
        this.initiatedBy = from;
        this.requestText = requestText;
        this.createdAt = Instant.now();
    }

    public FriendshipId getId() { return id; }
    public Long getUserAId() { return id.getUserAId(); }
    public Long getUserBId() { return id.getUserBId(); }
    public FriendshipStatus getStatus() { return status; }
    public Long getInitiatedBy() { return initiatedBy; }
    public String getRequestText() { return requestText; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getAcceptedAt() { return acceptedAt; }

    public Long otherUserId(Long viewer) {
        return viewer.equals(getUserAId()) ? getUserBId() : getUserAId();
    }

    public void accept() {
        this.status = FriendshipStatus.ACCEPTED;
        this.acceptedAt = Instant.now();
    }
}
