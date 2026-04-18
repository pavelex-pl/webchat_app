package com.webchat.friends.dto;

import com.webchat.friends.Friendship;
import com.webchat.friends.FriendshipStatus;
import java.time.Instant;

public record FriendshipResponse(
        Long userId,
        String username,
        FriendshipStatus status,
        boolean incoming,
        String requestText,
        Instant createdAt,
        Instant acceptedAt
) {
    public static FriendshipResponse of(Friendship f, Long viewerId, String otherUsername) {
        boolean incoming = f.getStatus() == FriendshipStatus.PENDING && !f.getInitiatedBy().equals(viewerId);
        return new FriendshipResponse(
                f.otherUserId(viewerId), otherUsername, f.getStatus(), incoming,
                f.getRequestText(), f.getCreatedAt(), f.getAcceptedAt());
    }
}
