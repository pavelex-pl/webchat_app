package com.webchat.friends;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBlockRepository extends JpaRepository<UserBlock, UserBlockId> {
    List<UserBlock> findByIdBlockerId(Long blockerId);
    boolean existsByIdBlockerIdAndIdBlockedId(Long blockerId, Long blockedId);

    default boolean eitherBlocks(Long u1, Long u2) {
        return existsByIdBlockerIdAndIdBlockedId(u1, u2) || existsByIdBlockerIdAndIdBlockedId(u2, u1);
    }
}
