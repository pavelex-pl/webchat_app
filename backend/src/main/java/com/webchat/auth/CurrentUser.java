package com.webchat.auth;

import java.util.UUID;

public record CurrentUser(Long userId, UUID sessionId) {}
