package com.webchat.friends;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class UserBlockId implements Serializable {

    @Column(name = "blocker_id")
    private Long blockerId;

    @Column(name = "blocked_id")
    private Long blockedId;

    protected UserBlockId() {}

    public UserBlockId(Long blockerId, Long blockedId) {
        this.blockerId = blockerId;
        this.blockedId = blockedId;
    }

    public Long getBlockerId() { return blockerId; }
    public Long getBlockedId() { return blockedId; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserBlockId that)) return false;
        return Objects.equals(blockerId, that.blockerId) && Objects.equals(blockedId, that.blockedId);
    }
    @Override public int hashCode() { return Objects.hash(blockerId, blockedId); }
}
