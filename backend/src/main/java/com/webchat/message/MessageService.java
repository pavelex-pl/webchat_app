package com.webchat.message;

import com.webchat.attachment.AttachmentService;
import com.webchat.chat.Chat;
import com.webchat.chat.ChatMember;
import com.webchat.chat.ChatPolicy;
import com.webchat.chat.ChatRepository;
import com.webchat.chat.ChatRole;
import com.webchat.chat.ChatType;
import com.webchat.chat.DirectChatService;
import com.webchat.common.BadRequestException;
import com.webchat.common.NotFoundException;
import com.webchat.common.UnauthorizedException;
import com.webchat.friends.FriendService;
import com.webchat.message.dto.SendMessageRequest;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageService {

    private static final int MAX_PAGE = 100;
    private static final int MAX_BYTES = 3072;

    private final MessageRepository messages;
    private final ChatRepository chats;
    private final ChatPolicy policy;
    private final ReadMarkerRepository readMarkers;
    private final ApplicationEventPublisher events;
    private final DirectChatService directs;
    private final FriendService friends;
    private final AttachmentService attachmentService;

    public MessageService(MessageRepository messages, ChatRepository chats, ChatPolicy policy,
                          ReadMarkerRepository readMarkers, ApplicationEventPublisher events,
                          DirectChatService directs, FriendService friends,
                          AttachmentService attachmentService) {
        this.messages = messages;
        this.chats = chats;
        this.policy = policy;
        this.readMarkers = readMarkers;
        this.events = events;
        this.directs = directs;
        this.friends = friends;
        this.attachmentService = attachmentService;
    }

    @Transactional
    public Message send(Long userId, Long chatId, String body, Long replyToId,
                        List<SendMessageRequest.AttachmentLink> attachments) {
        policy.requireMembership(chatId, userId);
        Chat chat = chats.findById(chatId).orElseThrow(() -> new NotFoundException("Chat not found"));
        if (chat.getType() == ChatType.DIRECT) {
            Long peerId = directs.peerId(chatId, userId);
            if (peerId == null || !friends.canMessage(userId, peerId)) {
                throw new UnauthorizedException("Direct chat is read-only: you are no longer friends or one side has blocked the other");
            }
        }
        boolean hasText = body != null && !body.isBlank();
        boolean hasAttachments = attachments != null && !attachments.isEmpty();
        if (!hasText && !hasAttachments) throw new BadRequestException("Message must have text or attachments");
        if (hasText) validateBody(body);
        if (replyToId != null) {
            Message ref = messages.findById(replyToId)
                    .orElseThrow(() -> new BadRequestException("Reply target not found"));
            if (!ref.getChatId().equals(chatId)) throw new BadRequestException("Reply target not in this chat");
        }
        Message m = new Message(chatId, userId,
                hasText ? body.trim() : null, replyToId);
        Message saved = messages.save(m);
        if (hasAttachments) {
            List<Long> ids = attachments.stream().map(SendMessageRequest.AttachmentLink::id).toList();
            Map<Long, String> commentsById = new HashMap<>();
            for (var a : attachments) {
                if (a.comment() != null) commentsById.put(a.id(), a.comment());
            }
            attachmentService.linkToMessage(userId, chatId, saved.getId(), ids, commentsById);
        }
        events.publishEvent(new MessageCreatedEvent(saved));
        return saved;
    }

    public List<Message> list(Long userId, Long chatId, Long before, int limit) {
        policy.requireMembership(chatId, userId);
        int lim = Math.min(Math.max(limit, 1), MAX_PAGE);
        if (before == null) return messages.findByChatIdOrderByIdDesc(chatId, Limit.of(lim));
        return messages.findByChatIdAndIdLessThanOrderByIdDesc(chatId, before, Limit.of(lim));
    }

    @Transactional
    public Message edit(Long userId, Long messageId, String newBody) {
        Message m = messages.findById(messageId).orElseThrow(() -> new NotFoundException("Message not found"));
        if (m.isDeleted()) throw new BadRequestException("Message is deleted");
        if (!userId.equals(m.getAuthorId())) throw new UnauthorizedException("Only the author can edit");
        validateBody(newBody);
        m.setBody(newBody.trim());
        m.setEditedAt(Instant.now());
        events.publishEvent(new MessageUpdatedEvent(m));
        return m;
    }

    @Transactional
    public Message delete(Long userId, Long messageId) {
        Message m = messages.findById(messageId).orElseThrow(() -> new NotFoundException("Message not found"));
        if (m.isDeleted()) return m;
        Chat c = chats.findById(m.getChatId()).orElseThrow(() -> new NotFoundException("Chat not found"));
        if (c.getType() == ChatType.DIRECT) {
            Long peerId = directs.peerId(m.getChatId(), userId);
            if (peerId == null || !friends.canMessage(userId, peerId)) {
                throw new UnauthorizedException("Direct chat is read-only: cannot delete messages");
            }
            if (!userId.equals(m.getAuthorId())) {
                throw new UnauthorizedException("Only the author can delete");
            }
        } else if (!userId.equals(m.getAuthorId())) {
            ChatMember caller = policy.requireMembership(m.getChatId(), userId);
            if (caller.getRole() == ChatRole.MEMBER) throw new UnauthorizedException("Admin role required");
        }
        m.setDeletedAt(Instant.now());
        m.setBody(null);
        events.publishEvent(new MessageUpdatedEvent(m));
        return m;
    }

    @Transactional
    public void markRead(Long userId, Long chatId, Long lastReadMessageId) {
        policy.requireMembership(chatId, userId);
        var existing = readMarkers.findById(new ReadMarkerId(chatId, userId));
        ReadMarker rm = existing.orElseGet(() -> new ReadMarker(chatId, userId, lastReadMessageId));
        Long prev = existing.map(ReadMarker::getLastReadMessageId).orElse(null);
        boolean advanced = prev == null || lastReadMessageId > prev;
        if (advanced) {
            rm.setLastReadMessageId(lastReadMessageId);
            readMarkers.save(rm);
            events.publishEvent(new MessageReadEvent(userId, chatId, lastReadMessageId));
        }
    }

    @Transactional
    public void markAllRead(Long userId, Long chatId) {
        policy.requireMembership(chatId, userId);
        messages.findMaxIdByChatId(chatId).ifPresent(maxId -> markRead(userId, chatId, maxId));
    }

    private static void validateBody(String body) {
        if (body == null || body.isBlank()) throw new BadRequestException("Message body required");
        if (body.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new BadRequestException("Message body exceeds 3 KB");
        }
    }
}
