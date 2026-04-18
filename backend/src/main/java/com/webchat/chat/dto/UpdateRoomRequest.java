package com.webchat.chat.dto;

import com.webchat.chat.ChatType;
import jakarta.validation.constraints.Size;

public record UpdateRoomRequest(
        @Size(min = 3, max = 64) String name,
        @Size(max = 2000) String description,
        ChatType type
) {}
