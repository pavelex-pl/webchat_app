package com.webchat.chat;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "chat_members")
public class ChatMember {

    @EmbeddedId
    private ChatMemberId id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRole role;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    protected ChatMember() {}

    public ChatMember(Long chatId, Long userId, ChatRole role) {
        this.id = new ChatMemberId(chatId, userId);
        this.role = role;
        this.joinedAt = Instant.now();
    }

    public ChatMemberId getId() { return id; }
    public Long getChatId() { return id.getChatId(); }
    public Long getUserId() { return id.getUserId(); }
    public ChatRole getRole() { return role; }
    public void setRole(ChatRole role) { this.role = role; }
    public Instant getJoinedAt() { return joinedAt; }
}
