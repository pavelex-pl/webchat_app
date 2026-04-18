package com.webchat.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record OpenDirectRequest(
        @NotBlank String username
) {}
