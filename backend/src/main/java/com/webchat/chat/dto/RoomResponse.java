package com.webchat.chat.dto;

import com.webchat.chat.Chat;
import com.webchat.chat.ChatType;

public record RoomResponse(
        Long id,
        ChatType type,
        String name,
        String description,
        Long ownerId,
        long memberCount,
        boolean bannedFromRoom
) {
    public static RoomResponse from(Chat c, long memberCount) {
        return new RoomResponse(c.getId(), c.getType(), c.getName(), c.getDescription(),
                c.getOwnerId(), memberCount, false);
    }

    public static RoomResponse from(Chat c, long memberCount, boolean bannedFromRoom) {
        return new RoomResponse(c.getId(), c.getType(), c.getName(), c.getDescription(),
                c.getOwnerId(), memberCount, bannedFromRoom);
    }
}
