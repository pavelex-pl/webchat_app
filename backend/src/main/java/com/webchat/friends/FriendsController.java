package com.webchat.friends;

import com.webchat.auth.CurrentUserResolver;
import com.webchat.auth.User;
import com.webchat.auth.UserLookup;
import com.webchat.auth.UserRepository;
import com.webchat.common.NotFoundException;
import com.webchat.friends.dto.FriendRequestBody;
import com.webchat.friends.dto.FriendshipResponse;
import com.webchat.friends.dto.UserSummary;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/friends")
public class FriendsController {

    private final FriendService friends;
    private final UserRepository users;
    private final UserLookup lookup;
    private final CurrentUserResolver currentUser;

    public FriendsController(FriendService friends, UserRepository users,
                             UserLookup lookup, CurrentUserResolver currentUser) {
        this.friends = friends;
        this.users = users;
        this.lookup = lookup;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<FriendshipResponse> list() {
        return assemble(friends.listFriends(currentUser.require().userId()));
    }

    @GetMapping("/requests/incoming")
    public List<FriendshipResponse> incoming() {
        return assemble(friends.listIncoming(currentUser.require().userId()));
    }

    @GetMapping("/requests/outgoing")
    public List<FriendshipResponse> outgoing() {
        return assemble(friends.listOutgoing(currentUser.require().userId()));
    }

    @PostMapping("/requests")
    public ResponseEntity<FriendshipResponse> send(@Valid @RequestBody FriendRequestBody req) {
        Long uid = currentUser.require().userId();
        Friendship f = friends.sendRequest(uid, req.username(), req.text());
        String otherName = lookup.usernameOr(f.otherUserId(uid), null);
        return ResponseEntity.status(HttpStatus.CREATED).body(FriendshipResponse.of(f, uid, otherName));
    }

    @PostMapping("/requests/{otherUserId}/accept")
    public ResponseEntity<Void> accept(@PathVariable Long otherUserId) {
        friends.accept(currentUser.require().userId(), otherUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/requests/{otherUserId}/decline")
    public ResponseEntity<Void> decline(@PathVariable Long otherUserId) {
        friends.decline(currentUser.require().userId(), otherUserId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/requests/{otherUserId}")
    public ResponseEntity<Void> cancelOutgoing(@PathVariable Long otherUserId) {
        friends.cancelOutgoing(currentUser.require().userId(), otherUserId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{otherUserId}")
    public ResponseEntity<Void> remove(@PathVariable Long otherUserId) {
        friends.remove(currentUser.require().userId(), otherUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/block/{otherUserId}")
    public ResponseEntity<Void> block(@PathVariable Long otherUserId) {
        friends.block(currentUser.require().userId(), otherUserId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/block/{otherUserId}")
    public ResponseEntity<Void> unblock(@PathVariable Long otherUserId) {
        friends.unblock(currentUser.require().userId(), otherUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/blocks")
    public List<UserSummary> listBlocks() {
        Long uid = currentUser.require().userId();
        List<UserBlock> list = friends.listBlocks(uid);
        Map<Long, String> names = lookup.usernames(list.stream().map(UserBlock::getBlockedId).collect(Collectors.toSet()));
        return list.stream()
                .map(b -> new UserSummary(b.getBlockedId(), names.get(b.getBlockedId()), b.getCreatedAt()))
                .toList();
    }

    @PostMapping("/block-by-username/{username}")
    public ResponseEntity<Void> blockByUsername(@PathVariable String username) {
        Long uid = currentUser.require().userId();
        User target = users.findByUsername(username)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException("User not found"));
        friends.block(uid, target.getId());
        return ResponseEntity.noContent().build();
    }

    private List<FriendshipResponse> assemble(List<Friendship> list) {
        Long uid = currentUser.require().userId();
        Set<Long> ids = list.stream().map(f -> f.otherUserId(uid)).collect(Collectors.toSet());
        Map<Long, String> names = lookup.usernames(ids);
        return list.stream()
                .map(f -> FriendshipResponse.of(f, uid, names.get(f.otherUserId(uid))))
                .toList();
    }
}
