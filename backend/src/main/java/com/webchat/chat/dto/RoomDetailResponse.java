package com.webchat.chat.dto;

import com.webchat.chat.Chat;
import com.webchat.chat.ChatRole;
import com.webchat.chat.ChatType;

public record RoomDetailResponse(
        Long id,
        ChatType type,
        String name,
        String description,
        Long ownerId,
        String ownerUsername,
        long memberCount,
        ChatRole yourRole
) {
    public static RoomDetailResponse from(Chat c, String ownerUsername, long memberCount, ChatRole yourRole) {
        return new RoomDetailResponse(c.getId(), c.getType(), c.getName(), c.getDescription(),
                c.getOwnerId(), ownerUsername, memberCount, yourRole);
    }
}
