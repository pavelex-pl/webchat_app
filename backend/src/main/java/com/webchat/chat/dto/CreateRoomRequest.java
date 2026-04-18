package com.webchat.chat.dto;

import com.webchat.chat.ChatType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRoomRequest(
        @NotNull ChatType type,
        @NotBlank @Size(min = 3, max = 64) String name,
        @Size(max = 2000) String description
) {}
