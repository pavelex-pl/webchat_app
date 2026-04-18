package com.webchat.presence.dto;

import com.webchat.presence.Status;

public record PresenceUpdate(long userId, Status status) {}
