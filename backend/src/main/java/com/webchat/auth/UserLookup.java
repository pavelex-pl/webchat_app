package com.webchat.auth;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class UserLookup {

    private final UserRepository users;

    public UserLookup(UserRepository users) {
        this.users = users;
    }

    public Map<Long, String> usernames(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return Map.of();
        return users.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));
    }

    public String usernameOr(Long userId, String fallback) {
        if (userId == null) return fallback;
        return users.findById(userId).map(User::getUsername).orElse(fallback);
    }

    public <R> R withUser(Long userId, Function<User, R> fn, R ifMissing) {
        if (userId == null) return ifMissing;
        return users.findById(userId).map(fn).orElse(ifMissing);
    }
}
