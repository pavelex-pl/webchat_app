package com.webchat.message;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "author_id")
    private Long authorId;

    @Column(name = "reply_to_id")
    private Long replyToId;

    @Column
    private String body;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "edited_at")
    private Instant editedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Message() {}

    public Message(Long chatId, Long authorId, String body, Long replyToId) {
        this.chatId = chatId;
        this.authorId = authorId;
        this.body = body;
        this.replyToId = replyToId;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getChatId() { return chatId; }
    public Long getAuthorId() { return authorId; }
    public Long getReplyToId() { return replyToId; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getEditedAt() { return editedAt; }
    public void setEditedAt(Instant editedAt) { this.editedAt = editedAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }

    public boolean isDeleted() { return deletedAt != null; }
}
