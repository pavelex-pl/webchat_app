package com.webchat.chat;

/**
 * Published when a chat's membership changes so other participants' UIs can
 * refresh the member list and derived state (member count, presence watches).
 */
public record ChatMembershipEvent(Long chatId, Long userId, Kind kind) {
    public enum Kind { JOINED, LEFT, BANNED }
}
