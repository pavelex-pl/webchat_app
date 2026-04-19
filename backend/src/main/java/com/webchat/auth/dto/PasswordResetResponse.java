package com.webchat.auth.dto;

/**
 * Returned from POST /api/auth/password-reset/request. Since this build has no
 * email service, the token is returned directly so the user can complete the
 * reset flow. {@code token} is {@code null} when the email is unknown.
 */
public record PasswordResetResponse(String token) {}
