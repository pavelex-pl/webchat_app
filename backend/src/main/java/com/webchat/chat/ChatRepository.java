package com.webchat.chat;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRepository extends JpaRepository<Chat, Long> {

    boolean existsByName(String name);

    Optional<Chat> findByName(String name);

    @Query("""
           SELECT c FROM Chat c
           WHERE c.type = com.webchat.chat.ChatType.PUBLIC_ROOM
             AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%'))
                  OR LOWER(COALESCE(c.description,'')) LIKE LOWER(CONCAT('%', :q, '%')))
           ORDER BY c.name ASC
           """)
    Page<Chat> searchPublic(@Param("q") String q, Pageable pageable);

    @Query("""
           SELECT c FROM Chat c
           WHERE c.type = :type
             AND c.id IN (SELECT m.id.chatId FROM ChatMember m WHERE m.id.userId = :userId)
           ORDER BY c.name ASC, c.id ASC
           """)
    Page<Chat> findRoomsForUserByType(@Param("userId") Long userId,
                                      @Param("type") ChatType type,
                                      Pageable pageable);

    @Query("""
           SELECT c FROM Chat c
           WHERE c.type = com.webchat.chat.ChatType.DIRECT
             AND c.id IN (SELECT m.id.chatId FROM ChatMember m WHERE m.id.userId = :userId)
           ORDER BY c.createdAt DESC, c.id DESC
           """)
    Page<Chat> findDirectChatsForUserPaged(@Param("userId") Long userId, Pageable pageable);

    @Query("""
           SELECT c FROM Chat c
           WHERE c.type = com.webchat.chat.ChatType.DIRECT
             AND c.id IN (SELECT m.id.chatId FROM ChatMember m WHERE m.id.userId = :u1)
             AND c.id IN (SELECT m.id.chatId FROM ChatMember m WHERE m.id.userId = :u2)
           """)
    java.util.Optional<Chat> findDirectChatBetween(@Param("u1") Long u1, @Param("u2") Long u2);
}
