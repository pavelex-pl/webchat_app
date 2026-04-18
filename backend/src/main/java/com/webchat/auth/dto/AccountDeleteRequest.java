package com.webchat.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record AccountDeleteRequest(
        @NotBlank String password
) {}
