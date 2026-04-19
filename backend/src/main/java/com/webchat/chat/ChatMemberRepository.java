package com.webchat.chat;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMemberRepository extends JpaRepository<ChatMember, ChatMemberId> {
    List<ChatMember> findByIdChatId(Long chatId);

    @Query("""
           SELECT m FROM ChatMember m
           WHERE m.id.chatId = :chatId
           ORDER BY m.role ASC, m.joinedAt ASC, m.id.userId ASC
           """)
    Page<ChatMember> findByChatIdOrdered(@Param("chatId") Long chatId, Pageable pageable);

    List<ChatMember> findByIdUserId(Long userId);
    Optional<ChatMember> findByIdChatIdAndIdUserId(Long chatId, Long userId);
    long countByIdChatId(Long chatId);
    boolean existsByIdChatIdAndIdUserId(Long chatId, Long userId);
    void deleteByIdChatIdAndIdUserId(Long chatId, Long userId);

    @Query("""
           SELECT m FROM ChatMember m
           WHERE m.id.chatId = :chatId
             AND m.role IN (com.webchat.chat.ChatRole.OWNER, com.webchat.chat.ChatRole.ADMIN)
           ORDER BY m.role ASC, m.joinedAt ASC, m.id.userId ASC
           """)
    List<ChatMember> findStaffByChatId(@Param("chatId") Long chatId);

    @Query("""
           SELECT m.id.chatId FROM ChatMember m
           WHERE m.id.userId = :userId
             AND m.id.chatId IN :chatIds
           """)
    List<Long> findMemberChatIds(@Param("userId") Long userId,
                                 @Param("chatIds") Collection<Long> chatIds);
}
