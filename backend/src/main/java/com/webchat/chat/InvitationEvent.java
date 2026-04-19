package com.webchat.chat;

/**
 * Fired after an invitation is created, accepted, or declined so other
 * components can broadcast a notification to the invitee.
 */
public record InvitationEvent(Long inviteeId, Long chatId, Long invitationId, Kind kind) {
    public enum Kind { CREATED, ACCEPTED, DECLINED }
}
