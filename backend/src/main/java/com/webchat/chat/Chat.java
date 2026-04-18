package com.webchat.chat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "chats")
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatType type;

    @Column
    private String name;

    @Column
    private String description;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Chat() {}

    public static Chat createRoom(ChatType type, String name, String description, Long ownerId) {
        if (type == ChatType.DIRECT) throw new IllegalArgumentException("Use createDirect for DM");
        Chat c = new Chat();
        c.type = type;
        c.name = name;
        c.description = description;
        c.ownerId = ownerId;
        c.createdAt = Instant.now();
        return c;
    }

    public static Chat createDirect() {
        Chat c = new Chat();
        c.type = ChatType.DIRECT;
        c.createdAt = Instant.now();
        return c;
    }

    public Long getId() { return id; }
    public ChatType getType() { return type; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getOwnerId() { return ownerId; }
    public void setType(ChatType type) { this.type = type; }
    public Instant getCreatedAt() { return createdAt; }

    public boolean isRoom() { return type != ChatType.DIRECT; }
}
