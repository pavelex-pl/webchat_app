package com.webchat.attachment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "attachments")
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id")
    private Long messageId;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "uploader_id")
    private Long uploaderId;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column
    private String comment;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Attachment() {}

    public Attachment(Long chatId, Long uploaderId, String originalName, String storagePath,
                      String mimeType, long sizeBytes) {
        this.chatId = chatId;
        this.uploaderId = uploaderId;
        this.originalName = originalName;
        this.storagePath = storagePath;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }
    public Long getChatId() { return chatId; }
    public Long getUploaderId() { return uploaderId; }
    public String getOriginalName() { return originalName; }
    public String getStoragePath() { return storagePath; }
    public String getMimeType() { return mimeType; }
    public long getSizeBytes() { return sizeBytes; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public Instant getCreatedAt() { return createdAt; }
}
