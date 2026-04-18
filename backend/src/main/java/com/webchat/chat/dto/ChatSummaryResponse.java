package com.webchat.chat.dto;

import com.webchat.chat.ChatType;

public record ChatSummaryResponse(
        Long id,
        ChatType type,
        String name,
        String description,
        Long ownerId,
        long memberCount,
        Long peerUserId,
        String peerUsername,
        long unreadCount
) {}
