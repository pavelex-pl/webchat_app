package com.webchat.chat.dto;

import com.webchat.chat.ChatRole;
import com.webchat.chat.ChatType;

public record ChatDetailResponse(
        Long id,
        ChatType type,
        String name,
        String description,
        Long ownerId,
        String ownerUsername,
        long memberCount,
        ChatRole yourRole,
        Long peerUserId,
        String peerUsername,
        Boolean canMessage,
        Long lastReadMessageId
) {}
