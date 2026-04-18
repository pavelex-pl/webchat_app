package com.webchat.chat;

import com.webchat.auth.User;
import com.webchat.auth.UserRepository;
import com.webchat.common.BadRequestException;
import com.webchat.common.NotFoundException;
import com.webchat.common.UnauthorizedException;
import com.webchat.friends.FriendService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DirectChatService {

    private final ChatRepository chats;
    private final ChatMemberRepository members;
    private final UserRepository users;
    private final FriendService friends;

    public DirectChatService(ChatRepository chats, ChatMemberRepository members,
                             UserRepository users, FriendService friends) {
        this.chats = chats;
        this.members = members;
        this.users = users;
        this.friends = friends;
    }

    @Transactional
    public Chat openOrCreate(Long selfId, String peerUsername) {
        User peer = users.findByUsername(peerUsername)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (peer.getId().equals(selfId)) throw new BadRequestException("Cannot DM yourself");
        if (!friends.canMessage(selfId, peer.getId())) {
            throw new UnauthorizedException("You can only message friends who have not blocked you");
        }
        return chats.findDirectChatBetween(selfId, peer.getId()).orElseGet(() -> {
            Chat c = chats.save(Chat.createDirect());
            members.save(new ChatMember(c.getId(), selfId, ChatRole.MEMBER));
            members.save(new ChatMember(c.getId(), peer.getId(), ChatRole.MEMBER));
            return c;
        });
    }

    public Long peerId(Long chatId, Long selfId) {
        return members.findByIdChatId(chatId).stream()
                .map(ChatMember::getUserId)
                .filter(id -> !id.equals(selfId))
                .findFirst()
                .orElse(null);
    }
}
