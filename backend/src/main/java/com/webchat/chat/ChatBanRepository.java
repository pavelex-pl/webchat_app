package com.webchat.chat;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatBanRepository extends JpaRepository<ChatBan, ChatBanId> {
    List<ChatBan> findByIdChatId(Long chatId);
    Optional<ChatBan> findByIdChatIdAndIdUserId(Long chatId, Long userId);
    boolean existsByIdChatIdAndIdUserId(Long chatId, Long userId);
    void deleteByIdChatIdAndIdUserId(Long chatId, Long userId);

    @Query("SELECT b.id.chatId FROM ChatBan b WHERE b.id.userId = :userId AND b.id.chatId IN :chatIds")
    List<Long> findBannedChatIds(@Param("userId") Long userId, @Param("chatIds") List<Long> chatIds);
}
