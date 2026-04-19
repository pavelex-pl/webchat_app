package com.webchat.chat;

import com.webchat.auth.CurrentUserResolver;
import com.webchat.auth.UserLookup;
import com.webchat.chat.dto.ChatDetailResponse;
import com.webchat.chat.dto.ChatSummaryResponse;
import com.webchat.chat.dto.OpenDirectRequest;
import com.webchat.chat.dto.PageResponse;
import com.webchat.common.BadRequestException;
import com.webchat.common.NotFoundException;
import com.webchat.friends.FriendService;
import com.webchat.message.MessageRepository;
import com.webchat.message.ReadMarkerId;
import com.webchat.message.ReadMarkerRepository;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chats")
public class ChatsController {

    private final ChatRepository chats;
    private final ChatMemberRepository members;
    private final MembershipService memberships;
    private final DirectChatService directs;
    private final FriendService friends;
    private final UserLookup lookup;
    private final MessageRepository messages;
    private final ReadMarkerRepository readMarkers;
    private final CurrentUserResolver currentUser;

    public ChatsController(ChatRepository chats, ChatMemberRepository members,
                           MembershipService memberships, DirectChatService directs,
                           FriendService friends, UserLookup lookup,
                           MessageRepository messages, ReadMarkerRepository readMarkers,
                           CurrentUserResolver currentUser) {
        this.chats = chats;
        this.members = members;
        this.memberships = memberships;
        this.directs = directs;
        this.friends = friends;
        this.lookup = lookup;
        this.messages = messages;
        this.readMarkers = readMarkers;
        this.currentUser = currentUser;
    }

    @GetMapping
    public PageResponse<ChatSummaryResponse> myChats(
            @RequestParam("type") String typeRaw,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Long uid = currentUser.require().userId();
        ChatType type;
        try {
            type = ChatType.valueOf(typeRaw);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown chat type: " + typeRaw);
        }

        Page<Chat> p = (type == ChatType.DIRECT)
                ? chats.findDirectChatsForUserPaged(uid, PageRequest.of(page, size))
                : chats.findRoomsForUserByType(uid, type, PageRequest.of(page, size));

        List<Chat> pageItems = p.getContent();
        Set<Long> pageChatIds = pageItems.stream().map(Chat::getId).collect(Collectors.toSet());

        Map<Long, Long> unreadByChat = new HashMap<>();
        if (!pageChatIds.isEmpty()) {
            for (var u : messages.countUnread(uid, pageChatIds)) {
                unreadByChat.put(u.getChatId(), u.getUnread());
            }
        }

        Map<Long, Long> peerIdByChat = new HashMap<>();
        if (type == ChatType.DIRECT) {
            for (Chat c : pageItems) {
                peerIdByChat.put(c.getId(), directs.peerId(c.getId(), uid));
            }
        }
        Map<Long, String> peerUsernames = peerIdByChat.isEmpty()
                ? Map.of()
                : lookup.usernames(peerIdByChat.values().stream()
                        .filter(java.util.Objects::nonNull).collect(Collectors.toSet()));

        List<ChatSummaryResponse> out = new ArrayList<>();
        for (Chat c : pageItems) {
            if (type == ChatType.DIRECT) {
                Long peerId = peerIdByChat.get(c.getId());
                out.add(new ChatSummaryResponse(
                        c.getId(), c.getType(), null, null, null,
                        memberships.memberCount(c.getId()),
                        peerId, peerId == null ? null : peerUsernames.get(peerId),
                        unreadByChat.getOrDefault(c.getId(), 0L)));
            } else {
                out.add(new ChatSummaryResponse(
                        c.getId(), c.getType(), c.getName(), c.getDescription(),
                        c.getOwnerId(), memberships.memberCount(c.getId()), null, null,
                        unreadByChat.getOrDefault(c.getId(), 0L)));
            }
        }
        return new PageResponse<>(out, p.getNumber(), p.getSize(), p.getTotalElements());
    }

    @GetMapping("/{id}")
    public ChatDetailResponse detail(@PathVariable Long id) {
        Long uid = currentUser.require().userId();
        Chat c = chats.findById(id).orElseThrow(() -> new NotFoundException("Chat not found"));
        if (!members.existsByIdChatIdAndIdUserId(id, uid) && c.getType() != ChatType.PUBLIC_ROOM) {
            throw new com.webchat.common.UnauthorizedException("No access to this chat");
        }
        ChatRole yourRole = members.findByIdChatIdAndIdUserId(id, uid).map(ChatMember::getRole).orElse(null);
        Long lastRead = readMarkers.findById(new ReadMarkerId(id, uid))
                .map(rm -> rm.getLastReadMessageId()).orElse(null);
        if (c.getType() == ChatType.DIRECT) {
            Long peerId = directs.peerId(id, uid);
            String peerUsername = peerId == null ? null : lookup.usernameOr(peerId, null);
            boolean canMsg = peerId != null && friends.canMessage(uid, peerId);
            return new ChatDetailResponse(
                    c.getId(), c.getType(), null, null, null, null,
                    memberships.memberCount(id), yourRole,
                    peerId, peerUsername, canMsg, lastRead);
        }
        return new ChatDetailResponse(
                c.getId(), c.getType(), c.getName(), c.getDescription(),
                c.getOwnerId(), lookup.usernameOr(c.getOwnerId(), null),
                memberships.memberCount(id), yourRole,
                null, null, null, lastRead);
    }

    @PostMapping("/direct")
    public ResponseEntity<ChatDetailResponse> openDirect(@Valid @RequestBody OpenDirectRequest req) {
        Long uid = currentUser.require().userId();
        Chat c = directs.openOrCreate(uid, req.username());
        return ResponseEntity.status(HttpStatus.CREATED).body(detail(c.getId()));
    }
}
