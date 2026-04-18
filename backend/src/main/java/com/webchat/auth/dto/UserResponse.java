package com.webchat.auth.dto;

import com.webchat.auth.User;
import java.time.Instant;

public record UserResponse(Long id, String email, String username, Instant createdAt) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getUsername(), u.getCreatedAt());
    }
}
