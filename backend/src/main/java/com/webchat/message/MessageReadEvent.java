package com.webchat.message;

public record MessageReadEvent(Long userId, Long chatId, Long lastReadMessageId) {}