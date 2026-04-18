package com.webchat.presence;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * In-memory registry of who is watching whose presence.
 * - watchersOf: target userId → watcher userIds
 * - perSession: wsSessionId → (watcherId, Set&lt;targetIds&gt;, Set&lt;tabIds&gt;) so we can clean up on disconnect.
 */
@Component
public class PresenceSubscriptions {

    private final Map<Long, Set<Long>> watchersByTarget = new ConcurrentHashMap<>();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public void addWatches(String wsSessionId, long watcherId, Set<Long> targets) {
        Session s = sessions.computeIfAbsent(wsSessionId, id -> new Session(watcherId));
        synchronized (s) {
            s.watching.addAll(targets);
        }
        for (Long t : targets) {
            watchersByTarget.computeIfAbsent(t, k -> ConcurrentHashMap.newKeySet()).add(watcherId);
        }
    }

    public void removeWatches(String wsSessionId, long watcherId, Set<Long> targets) {
        Session s = sessions.get(wsSessionId);
        if (s != null) synchronized (s) { s.watching.removeAll(targets); }
        for (Long t : targets) {
            Set<Long> set = watchersByTarget.get(t);
            if (set != null) { set.remove(watcherId); if (set.isEmpty()) watchersByTarget.remove(t); }
        }
    }

    public void registerTab(String wsSessionId, long userId, String tabId) {
        Session s = sessions.computeIfAbsent(wsSessionId, id -> new Session(userId));
        synchronized (s) { s.tabs.add(tabId); }
    }

    /** Returns watched targets and tabs this session had, then removes the session. */
    public DisconnectInfo removeSession(String wsSessionId) {
        Session s = sessions.remove(wsSessionId);
        if (s == null) return new DisconnectInfo(0L, Set.of(), Set.of());
        Set<Long> wasWatching;
        Set<String> tabs;
        synchronized (s) { wasWatching = new HashSet<>(s.watching); tabs = new HashSet<>(s.tabs); }
        for (Long t : wasWatching) {
            Set<Long> set = watchersByTarget.get(t);
            if (set != null) { set.remove(s.userId); if (set.isEmpty()) watchersByTarget.remove(t); }
        }
        return new DisconnectInfo(s.userId, tabs, wasWatching);
    }

    public Set<Long> watchersOf(long targetId) {
        Set<Long> set = watchersByTarget.get(targetId);
        return set == null ? Collections.emptySet() : Set.copyOf(set);
    }

    public record DisconnectInfo(long userId, Set<String> tabs, Set<Long> wasWatching) {}

    private static final class Session {
        final long userId;
        final Set<Long> watching = new HashSet<>();
        final Set<String> tabs = new HashSet<>();
        Session(long userId) { this.userId = userId; }
    }
}
