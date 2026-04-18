package com.webchat.chat.dto;

import java.time.Instant;

public record InvitationResponse(
        Long id,
        Long chatId,
        String chatName,
        Long inviteeId,
        String inviteeUsername,
        Long invitedByUserId,
        String invitedByUsername,
        Instant createdAt
) {}
