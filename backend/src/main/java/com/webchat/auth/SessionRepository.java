package com.webchat.auth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<Session, UUID> {
    List<Session> findByUserIdAndRevokedAtIsNullOrderByLastSeenAtDesc(Long userId);
    Optional<Session> findByIdAndUserId(UUID id, Long userId);
    Optional<Session> findByRefreshHash(String refreshHash);
}
