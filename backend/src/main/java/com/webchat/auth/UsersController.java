package com.webchat.auth;

import com.webchat.common.NotFoundException;
import com.webchat.friends.dto.UserSummary;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UsersController {

    private final UserRepository users;
    private final CurrentUserResolver currentUser;

    public UsersController(UserRepository users, CurrentUserResolver currentUser) {
        this.users = users;
        this.currentUser = currentUser;
    }

    @GetMapping("/search")
    public List<UserSummary> search(@RequestParam(required = false) String q) {
        currentUser.require();
        if (q == null || q.isBlank()) return List.of();
        String needle = q.trim().toLowerCase();
        return users.findAll(PageRequest.of(0, 20)).getContent().stream()
                .filter(u -> u.getDeletedAt() == null)
                .filter(u -> u.getUsername().toLowerCase().contains(needle))
                .map(u -> new UserSummary(u.getId(), u.getUsername(), u.getCreatedAt()))
                .toList();
    }

    @GetMapping("/{username}")
    public UserSummary byUsername(@PathVariable String username) {
        currentUser.require();
        User u = users.findByUsername(username)
                .filter(x -> x.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return new UserSummary(u.getId(), u.getUsername(), u.getCreatedAt());
    }
}
