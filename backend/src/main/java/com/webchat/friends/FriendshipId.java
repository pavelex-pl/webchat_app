package com.webchat.friends;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class FriendshipId implements Serializable {

    @Column(name = "user_a_id")
    private Long userAId;

    @Column(name = "user_b_id")
    private Long userBId;

    protected FriendshipId() {}

    public FriendshipId(Long userAId, Long userBId) {
        this.userAId = userAId;
        this.userBId = userBId;
    }

    public static FriendshipId canonical(Long u1, Long u2) {
        if (u1 < u2) return new FriendshipId(u1, u2);
        return new FriendshipId(u2, u1);
    }

    public Long getUserAId() { return userAId; }
    public Long getUserBId() { return userBId; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FriendshipId that)) return false;
        return Objects.equals(userAId, that.userAId) && Objects.equals(userBId, that.userBId);
    }
    @Override public int hashCode() { return Objects.hash(userAId, userBId); }
}
