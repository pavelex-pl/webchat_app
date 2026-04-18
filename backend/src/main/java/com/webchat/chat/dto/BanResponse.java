package com.webchat.chat.dto;

import java.time.Instant;

public record BanResponse(
        Long userId,
        String username,
        Long bannedByUserId,
        String bannedByUsername,
        Instant bannedAt
) {}
