package com.webchat.message;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "read_markers")
public class ReadMarker {

    @EmbeddedId
    private ReadMarkerId id;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ReadMarker() {}

    public ReadMarker(Long chatId, Long userId, Long lastReadMessageId) {
        this.id = new ReadMarkerId(chatId, userId);
        this.lastReadMessageId = lastReadMessageId;
        this.updatedAt = Instant.now();
    }

    public ReadMarkerId getId() { return id; }
    public Long getLastReadMessageId() { return lastReadMessageId; }
    public void setLastReadMessageId(Long lastReadMessageId) {
        this.lastReadMessageId = lastReadMessageId;
        this.updatedAt = Instant.now();
    }
    public Instant getUpdatedAt() { return updatedAt; }
}
