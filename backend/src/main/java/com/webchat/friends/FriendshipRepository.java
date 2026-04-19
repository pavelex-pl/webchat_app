package com.webchat.friends;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendshipRepository extends JpaRepository<Friendship, FriendshipId> {

    @Query("""
           SELECT f FROM Friendship f, com.webchat.auth.User u
           WHERE (f.id.userAId = :userId OR f.id.userBId = :userId)
             AND u.id = CASE WHEN f.id.userAId = :userId THEN f.id.userBId ELSE f.id.userAId END
             AND f.status = com.webchat.friends.FriendshipStatus.ACCEPTED
           ORDER BY LOWER(u.username) ASC
           """)
    List<Friendship> findAcceptedFor(@Param("userId") Long userId);

    @Query("""
           SELECT f FROM Friendship f, com.webchat.auth.User u
           WHERE f.status = com.webchat.friends.FriendshipStatus.PENDING
             AND (f.id.userAId = :userId OR f.id.userBId = :userId)
             AND f.initiatedBy <> :userId
             AND u.id = CASE WHEN f.id.userAId = :userId THEN f.id.userBId ELSE f.id.userAId END
           ORDER BY LOWER(u.username) ASC
           """)
    List<Friendship> findIncomingPending(@Param("userId") Long userId);

    @Query("""
           SELECT f FROM Friendship f, com.webchat.auth.User u
           WHERE f.status = com.webchat.friends.FriendshipStatus.PENDING
             AND f.initiatedBy = :userId
             AND u.id = CASE WHEN f.id.userAId = :userId THEN f.id.userBId ELSE f.id.userAId END
           ORDER BY LOWER(u.username) ASC
           """)
    List<Friendship> findOutgoingPending(@Param("userId") Long userId);

    default Optional<Friendship> findBetween(Long u1, Long u2) {
        return findById(FriendshipId.canonical(u1, u2));
    }
}
