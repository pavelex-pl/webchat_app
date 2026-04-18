package com.webchat.chat;

import com.webchat.common.BadRequestException;
import com.webchat.common.ConflictException;
import com.webchat.common.NotFoundException;
import com.webchat.common.UnauthorizedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomService {

    private static final int NAME_MIN = 3;
    private static final int NAME_MAX = 64;

    private final ChatRepository chats;
    private final ChatMemberRepository members;
    private final ChatPolicy policy;

    public RoomService(ChatRepository chats, ChatMemberRepository members, ChatPolicy policy) {
        this.chats = chats;
        this.members = members;
        this.policy = policy;
    }

    @Transactional
    public Chat create(Long ownerId, ChatType type, String name, String description) {
        if (type == ChatType.DIRECT) throw new BadRequestException("Cannot create DIRECT chat here");
        validateName(name);
        if (chats.existsByName(name)) throw new ConflictException("Room name already taken");
        Chat c = Chat.createRoom(type, name, description, ownerId);
        Chat saved = chats.save(c);
        members.save(new ChatMember(saved.getId(), ownerId, ChatRole.OWNER));
        return saved;
    }

    @Transactional
    public Chat update(Long userId, Long chatId, String name, String description, ChatType type) {
        Chat c = policy.requireOwnership(chatId, userId);
        if (name != null && !name.equals(c.getName())) {
            validateName(name);
            if (chats.existsByName(name)) throw new ConflictException("Room name already taken");
            c.setName(name);
        }
        if (description != null) c.setDescription(description);
        if (type != null && type != c.getType()) {
            if (type == ChatType.DIRECT) throw new BadRequestException("Cannot change to DIRECT");
            c.setType(type);
        }
        return c;
    }

    @Transactional
    public void delete(Long userId, Long chatId) {
        Chat c = policy.requireOwnership(chatId, userId);
        chats.delete(c);
    }

    public Chat getForUser(Long userId, Long chatId) {
        Chat c = chats.findById(chatId).orElseThrow(() -> new NotFoundException("Room not found"));
        if (c.getType() == ChatType.PUBLIC_ROOM) return c;
        if (c.getType() == ChatType.PRIVATE_ROOM || c.getType() == ChatType.DIRECT) {
            if (!members.existsByIdChatIdAndIdUserId(chatId, userId)) {
                throw new UnauthorizedException("No access to this chat");
            }
        }
        return c;
    }

    public Page<Chat> publicCatalog(String q, int page, int size) {
        if (size > 100) size = 100;
        return chats.searchPublic(q == null || q.isBlank() ? null : q.trim(), PageRequest.of(page, size));
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) throw new BadRequestException("Room name required");
        if (name.length() < NAME_MIN || name.length() > NAME_MAX) {
            throw new BadRequestException("Room name must be " + NAME_MIN + "-" + NAME_MAX + " chars");
        }
    }
}
