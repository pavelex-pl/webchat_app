package com.webchat.message.dto;

import jakarta.validation.constraints.NotNull;

public record ReadMarkerRequest(
        @NotNull Long lastReadMessageId
) {}
