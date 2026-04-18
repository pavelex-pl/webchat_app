package com.webchat.chat.dto;

import com.webchat.chat.ChatRole;
import java.time.Instant;

public record MemberResponse(
        Long userId,
        String username,
        ChatRole role,
        Instant joinedAt
) {}
