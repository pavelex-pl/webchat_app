package com.webchat.friends;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserBlockRepository extends JpaRepository<UserBlock, UserBlockId> {
    @Query("""
           SELECT b FROM UserBlock b, com.webchat.auth.User u
           WHERE b.id.blockerId = :blockerId AND u.id = b.id.blockedId
           ORDER BY LOWER(u.username) ASC
           """)
    List<UserBlock> findByIdBlockerId(@Param("blockerId") Long blockerId);
    boolean existsByIdBlockerIdAndIdBlockedId(Long blockerId, Long blockedId);

    default boolean eitherBlocks(Long u1, Long u2) {
        return existsByIdBlockerIdAndIdBlockedId(u1, u2) || existsByIdBlockerIdAndIdBlockedId(u2, u1);
    }
}
