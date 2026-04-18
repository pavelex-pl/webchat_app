package com.webchat.chat;

import com.webchat.auth.CurrentUserResolver;
import com.webchat.auth.UserLookup;
import com.webchat.chat.dto.InvitationResponse;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invitations")
public class InvitationsController {

    private final InvitationService invitations;
    private final ChatRepository chats;
    private final UserLookup lookup;
    private final CurrentUserResolver currentUser;

    public InvitationsController(InvitationService invitations, ChatRepository chats,
                                 UserLookup lookup, CurrentUserResolver currentUser) {
        this.invitations = invitations;
        this.chats = chats;
        this.lookup = lookup;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<InvitationResponse> list() {
        Long uid = currentUser.require().userId();
        List<ChatInvitation> list = invitations.listForUser(uid);
        var chatIds = list.stream().map(ChatInvitation::getChatId).collect(Collectors.toSet());
        Map<Long, String> chatNames = chats.findAllById(chatIds).stream()
                .collect(Collectors.toMap(Chat::getId, c -> c.getName() == null ? "" : c.getName()));
        var userIds = list.stream()
                .flatMap(i -> Stream.of(i.getInviteeId(), i.getInvitedBy()))
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> names = lookup.usernames(userIds);
        return list.stream().map(i -> new InvitationResponse(
                i.getId(), i.getChatId(), chatNames.get(i.getChatId()),
                i.getInviteeId(), names.get(i.getInviteeId()),
                i.getInvitedBy(), i.getInvitedBy() == null ? null : names.get(i.getInvitedBy()),
                i.getCreatedAt()
        )).toList();
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<Void> accept(@PathVariable Long id) {
        invitations.accept(currentUser.require().userId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/decline")
    public ResponseEntity<Void> decline(@PathVariable Long id) {
        invitations.decline(currentUser.require().userId(), id);
        return ResponseEntity.noContent().build();
    }
}
