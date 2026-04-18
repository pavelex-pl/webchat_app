package com.webchat.presence.dto;

import java.util.List;

public record WatchRequest(List<Long> userIds) {}
