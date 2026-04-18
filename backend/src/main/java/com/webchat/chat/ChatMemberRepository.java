package com.webchat.chat;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMemberRepository extends JpaRepository<ChatMember, ChatMemberId> {
    List<ChatMember> findByIdChatId(Long chatId);
    List<ChatMember> findByIdUserId(Long userId);
    Optional<ChatMember> findByIdChatIdAndIdUserId(Long chatId, Long userId);
    long countByIdChatId(Long chatId);
    boolean existsByIdChatIdAndIdUserId(Long chatId, Long userId);
    void deleteByIdChatIdAndIdUserId(Long chatId, Long userId);
}
