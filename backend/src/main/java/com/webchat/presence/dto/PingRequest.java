package com.webchat.presence.dto;

public record PingRequest(String tabId, long lastActivityAt) {}
