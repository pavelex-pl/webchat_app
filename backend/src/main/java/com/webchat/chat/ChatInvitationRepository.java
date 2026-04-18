package com.webchat.chat;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatInvitationRepository extends JpaRepository<ChatInvitation, Long> {
    List<ChatInvitation> findByInviteeIdAndAcceptedAtIsNullAndDeclinedAtIsNullOrderByCreatedAtDesc(Long inviteeId);
    List<ChatInvitation> findByChatIdAndAcceptedAtIsNullAndDeclinedAtIsNullOrderByCreatedAtDesc(Long chatId);
    Optional<ChatInvitation> findByChatIdAndInviteeIdAndAcceptedAtIsNullAndDeclinedAtIsNull(Long chatId, Long inviteeId);
}
