package com.webchat.friends;

/**
 * Fired after a friendship state change so the recipient's UI can refresh
 * friend request counts / lists in real time.
 */
public record FriendRequestEvent(Long recipientId, Long otherUserId, Kind kind) {
    public enum Kind { CREATED, ACCEPTED, DECLINED, CANCELED }
}
