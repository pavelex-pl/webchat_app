package com.webchat.chat;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ChatBanId implements Serializable {

    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "user_id")
    private Long userId;

    protected ChatBanId() {}

    public ChatBanId(Long chatId, Long userId) {
        this.chatId = chatId;
        this.userId = userId;
    }

    public Long getChatId() { return chatId; }
    public Long getUserId() { return userId; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChatBanId that)) return false;
        return Objects.equals(chatId, that.chatId) && Objects.equals(userId, that.userId);
    }

    @Override public int hashCode() { return Objects.hash(chatId, userId); }
}
