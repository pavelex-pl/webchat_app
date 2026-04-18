package com.webchat.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record InviteRequest(
        @NotBlank String username
) {}
