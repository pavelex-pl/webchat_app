package com.webchat.chat;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "chat_bans")
public class ChatBan {

    @EmbeddedId
    private ChatBanId id;

    @Column(name = "banned_by")
    private Long bannedBy;

    @Column(name = "banned_at", nullable = false)
    private Instant bannedAt;

    protected ChatBan() {}

    public ChatBan(Long chatId, Long userId, Long bannedBy) {
        this.id = new ChatBanId(chatId, userId);
        this.bannedBy = bannedBy;
        this.bannedAt = Instant.now();
    }

    public ChatBanId getId() { return id; }
    public Long getChatId() { return id.getChatId(); }
    public Long getUserId() { return id.getUserId(); }
    public Long getBannedBy() { return bannedBy; }
    public Instant getBannedAt() { return bannedAt; }
}
