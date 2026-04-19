package com.webchat.chat;

import com.webchat.auth.CurrentUserResolver;
import com.webchat.auth.UserLookup;
import com.webchat.chat.dto.BanResponse;
import com.webchat.chat.dto.CreateRoomRequest;
import com.webchat.chat.dto.InvitationResponse;
import com.webchat.chat.dto.InviteRequest;
import com.webchat.chat.dto.MemberResponse;
import com.webchat.chat.dto.PageResponse;
import com.webchat.chat.dto.RoomDetailResponse;
import com.webchat.chat.dto.RoomResponse;
import com.webchat.chat.dto.UpdateRoomRequest;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class RoomsController {

    private final RoomService rooms;
    private final MembershipService memberships;
    private final InvitationService invitations;
    private final ChatMemberRepository members;
    private final ChatBanRepository bans;
    private final UserLookup lookup;
    private final CurrentUserResolver currentUser;

    public RoomsController(RoomService rooms, MembershipService memberships, InvitationService invitations,
                           ChatMemberRepository members, ChatBanRepository bans, UserLookup lookup,
                           CurrentUserResolver currentUser) {
        this.rooms = rooms;
        this.memberships = memberships;
        this.invitations = invitations;
        this.members = members;
        this.bans = bans;
        this.lookup = lookup;
        this.currentUser = currentUser;
    }

    // ---- catalog & CRUD ----

    @GetMapping("/public")
    public PageResponse<RoomResponse> publicCatalog(@RequestParam(required = false) String q,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "50") int size) {
        Long uid = currentUser.require().userId();
        var p = rooms.publicCatalog(q, page, size);
        List<Long> pageChatIds = p.getContent().stream().map(Chat::getId).toList();
        Set<Long> bannedHere = pageChatIds.isEmpty()
                ? Set.of()
                : new HashSet<>(bans.findBannedChatIds(uid, pageChatIds));
        Set<Long> joinedHere = pageChatIds.isEmpty()
                ? Set.of()
                : new HashSet<>(members.findMemberChatIds(uid, pageChatIds));
        return PageResponse.of(p, c -> RoomResponse.from(
                c, memberships.memberCount(c.getId()),
                bannedHere.contains(c.getId()),
                joinedHere.contains(c.getId())));
    }

    @PostMapping
    public ResponseEntity<RoomResponse> create(@Valid @RequestBody CreateRoomRequest req) {
        Long uid = currentUser.require().userId();
        Chat c = rooms.create(uid, req.type(), req.name(), req.description());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RoomResponse.from(c, 1));
    }

    @GetMapping("/{id}")
    public RoomDetailResponse get(@PathVariable Long id) {
        Long uid = currentUser.require().userId();
        Chat c = rooms.getForUser(uid, id);
        ChatRole yourRole = members.findByIdChatIdAndIdUserId(id, uid)
                .map(ChatMember::getRole).orElse(null);
        return RoomDetailResponse.from(
                c,
                lookup.usernameOr(c.getOwnerId(), null),
                memberships.memberCount(id),
                yourRole
        );
    }

    @PatchMapping("/{id}")
    public RoomDetailResponse update(@PathVariable Long id, @Valid @RequestBody UpdateRoomRequest req) {
        Long uid = currentUser.require().userId();
        Chat c = rooms.update(uid, id, req.name(), req.description(), req.type());
        ChatRole yourRole = members.findByIdChatIdAndIdUserId(id, uid)
                .map(ChatMember::getRole).orElse(null);
        return RoomDetailResponse.from(c, lookup.usernameOr(c.getOwnerId(), null),
                memberships.memberCount(id), yourRole);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        rooms.delete(currentUser.require().userId(), id);
        return ResponseEntity.noContent().build();
    }

    // ---- join/leave ----

    @PostMapping("/{id}/join")
    public ResponseEntity<Void> join(@PathVariable Long id) {
        memberships.joinPublic(currentUser.require().userId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<Void> leave(@PathVariable Long id) {
        memberships.leave(currentUser.require().userId(), id);
        return ResponseEntity.noContent().build();
    }

    // ---- members & admins ----

    @GetMapping("/{id}/members")
    public PageResponse<MemberResponse> listMembers(@PathVariable Long id,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "50") int size) {
        Long uid = currentUser.require().userId();
        var p = memberships.listPage(uid, id, PageRequest.of(page, size));
        Map<Long, String> names = lookup.usernames(
                p.getContent().stream().map(ChatMember::getUserId).collect(Collectors.toSet()));
        return PageResponse.of(p, m -> new MemberResponse(
                m.getUserId(), names.get(m.getUserId()), m.getRole(), m.getJoinedAt()));
    }

    @GetMapping("/{id}/staff")
    public List<MemberResponse> listStaff(@PathVariable Long id) {
        Long uid = currentUser.require().userId();
        List<ChatMember> list = memberships.listStaff(uid, id);
        Map<Long, String> names = lookup.usernames(
                list.stream().map(ChatMember::getUserId).collect(Collectors.toSet()));
        return list.stream()
                .map(m -> new MemberResponse(m.getUserId(), names.get(m.getUserId()), m.getRole(), m.getJoinedAt()))
                .toList();
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> kick(@PathVariable Long id, @PathVariable Long userId) {
        memberships.kickAndBan(currentUser.require().userId(), id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/admins/{userId}")
    public ResponseEntity<Void> promoteAdmin(@PathVariable Long id, @PathVariable Long userId) {
        memberships.promoteToAdmin(currentUser.require().userId(), id, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/admins/{userId}")
    public ResponseEntity<Void> demoteAdmin(@PathVariable Long id, @PathVariable Long userId) {
        memberships.demoteAdmin(currentUser.require().userId(), id, userId);
        return ResponseEntity.noContent().build();
    }

    // ---- bans ----

    @GetMapping("/{id}/bans")
    public List<BanResponse> listBans(@PathVariable Long id) {
        Long uid = currentUser.require().userId();
        List<ChatBan> list = memberships.listBans(uid, id);
        Set<Long> ids = new java.util.HashSet<>();
        list.forEach(b -> {
            ids.add(b.getUserId());
            if (b.getBannedBy() != null) ids.add(b.getBannedBy());
        });
        Map<Long, String> names = lookup.usernames(ids);
        return list.stream()
                .map(b -> new BanResponse(
                        b.getUserId(), names.get(b.getUserId()),
                        b.getBannedBy(), b.getBannedBy() == null ? null : names.get(b.getBannedBy()),
                        b.getBannedAt()
                ))
                .toList();
    }

    @DeleteMapping("/{id}/bans/{userId}")
    public ResponseEntity<Void> unban(@PathVariable Long id, @PathVariable Long userId) {
        memberships.unban(currentUser.require().userId(), id, userId);
        return ResponseEntity.noContent().build();
    }

    // ---- invitations (per-room) ----

    @PostMapping("/{id}/invitations")
    @SuppressWarnings("unused")
    public ResponseEntity<InvitationResponse> invite(@PathVariable Long id, @Valid @RequestBody InviteRequest req) {
        Long uid = currentUser.require().userId();
        ChatInvitation inv = invitations.invite(uid, id, req.username());
        Map<Long, String> names = new HashMap<>(lookup.usernames(List.of(inv.getInviteeId(),
                inv.getInvitedBy() == null ? uid : inv.getInvitedBy())));
        Chat chat = rooms.getForUser(uid, inv.getChatId());
        return ResponseEntity.status(HttpStatus.CREATED).body(new InvitationResponse(
                inv.getId(), inv.getChatId(), chat.getName(),
                inv.getInviteeId(), names.get(inv.getInviteeId()),
                inv.getInvitedBy(), inv.getInvitedBy() == null ? null : names.get(inv.getInvitedBy()),
                inv.getCreatedAt()
        ));
    }

    @GetMapping("/{id}/invitations")
    public List<InvitationResponse> listRoomInvitations(@PathVariable Long id) {
        Long uid = currentUser.require().userId();
        List<ChatInvitation> list = invitations.listForChat(uid, id);
        Set<Long> ids = list.stream()
                .flatMap(i -> java.util.stream.Stream.of(i.getInviteeId(), i.getInvitedBy()))
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> names = lookup.usernames(ids);
        Chat chat = rooms.getForUser(uid, id);
        return list.stream()
                .map(i -> new InvitationResponse(
                        i.getId(), i.getChatId(), chat.getName(),
                        i.getInviteeId(), names.get(i.getInviteeId()),
                        i.getInvitedBy(), i.getInvitedBy() == null ? null : names.get(i.getInvitedBy()),
                        i.getCreatedAt()))
                .toList();
    }
}
