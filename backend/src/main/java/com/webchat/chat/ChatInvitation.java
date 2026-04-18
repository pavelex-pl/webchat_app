package com.webchat.chat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "chat_invitations")
public class ChatInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "invitee_id", nullable = false)
    private Long inviteeId;

    @Column(name = "invited_by")
    private Long invitedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "declined_at")
    private Instant declinedAt;

    protected ChatInvitation() {}

    public ChatInvitation(Long chatId, Long inviteeId, Long invitedBy) {
        this.chatId = chatId;
        this.inviteeId = inviteeId;
        this.invitedBy = invitedBy;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getChatId() { return chatId; }
    public Long getInviteeId() { return inviteeId; }
    public Long getInvitedBy() { return invitedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public Instant getDeclinedAt() { return declinedAt; }

    public boolean isPending() { return acceptedAt == null && declinedAt == null; }

    public void accept() { this.acceptedAt = Instant.now(); }
    public void decline() { this.declinedAt = Instant.now(); }
}
