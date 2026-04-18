package com.webchat.friends.dto;

import java.time.Instant;

public record UserSummary(
        Long id,
        String username,
        Instant createdAt
) {}
