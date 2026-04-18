package com.webchat.chat;

import com.webchat.common.BadRequestException;
import com.webchat.common.NotFoundException;
import com.webchat.common.UnauthorizedException;
import org.springframework.stereotype.Component;

@Component
public class ChatPolicy {

    private final ChatRepository chats;
    private final ChatMemberRepository members;

    public ChatPolicy(ChatRepository chats, ChatMemberRepository members) {
        this.chats = chats;
        this.members = members;
    }

    public Chat requireRoom(Long chatId) {
        Chat c = chats.findById(chatId).orElseThrow(() -> new NotFoundException("Room not found"));
        if (c.getType() == ChatType.DIRECT) throw new BadRequestException("Not a room");
        return c;
    }

    public ChatMember requireMembership(Long chatId, Long userId) {
        return members.findByIdChatIdAndIdUserId(chatId, userId)
                .orElseThrow(() -> new UnauthorizedException("Not a member of this chat"));
    }

    public ChatMember requireAdmin(Long chatId, Long userId) {
        ChatMember m = requireMembership(chatId, userId);
        if (m.getRole() == ChatRole.MEMBER) {
            throw new UnauthorizedException("Admin role required");
        }
        return m;
    }

    public Chat requireOwnership(Long chatId, Long userId) {
        Chat c = requireRoom(chatId);
        if (!userId.equals(c.getOwnerId())) {
            throw new UnauthorizedException("Only the owner can do this");
        }
        return c;
    }
}
