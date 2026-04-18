package com.webchat.message;

import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByChatIdAndIdLessThanOrderByIdDesc(Long chatId, Long before, Limit limit);
    List<Message> findByChatIdOrderByIdDesc(Long chatId, Limit limit);

    /**
     * Per-chat unread count for a user: messages not authored by them,
     * not deleted, with id greater than their read marker.
     */
    @Query(value = """
           SELECT m.chat_id AS chatId, COUNT(*) AS unread
           FROM messages m
           LEFT JOIN read_markers rm ON rm.chat_id = m.chat_id AND rm.user_id = :userId
           WHERE m.chat_id IN (:chatIds)
             AND m.deleted_at IS NULL
             AND (m.author_id IS NULL OR m.author_id <> :userId)
             AND m.id > COALESCE(rm.last_read_message_id, 0)
           GROUP BY m.chat_id
           """, nativeQuery = true)
    List<UnreadCount> countUnread(@Param("userId") Long userId,
                                  @Param("chatIds") Collection<Long> chatIds);

    interface UnreadCount {
        Long getChatId();
        Long getUnread();
    }
}
