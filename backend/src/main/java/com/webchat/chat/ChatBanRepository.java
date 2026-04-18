package com.webchat.chat;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatBanRepository extends JpaRepository<ChatBan, ChatBanId> {
    List<ChatBan> findByIdChatId(Long chatId);
    Optional<ChatBan> findByIdChatIdAndIdUserId(Long chatId, Long userId);
    boolean existsByIdChatIdAndIdUserId(Long chatId, Long userId);
    void deleteByIdChatIdAndIdUserId(Long chatId, Long userId);
}
