package com.webchat.presence;

import com.webchat.presence.dto.PresenceUpdate;
import java.util.Set;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PresenceScheduler {

    private final PresenceService presence;
    private final PresenceSubscriptions subs;
    private final SimpMessagingTemplate stomp;

    public PresenceScheduler(PresenceService presence, PresenceSubscriptions subs, SimpMessagingTemplate stomp) {
        this.presence = presence;
        this.subs = subs;
        this.stomp = stomp;
    }

    @Scheduled(fixedDelay = 1000)
    public void tick() {
        Set<String> dirty = presence.drainDirty();
        if (dirty == null || dirty.isEmpty()) return;
        for (String idStr : dirty) {
            long userId;
            try { userId = Long.parseLong(idStr); } catch (NumberFormatException e) { continue; }
            Status current = presence.compute(userId);
            Status cached = presence.cachedStatus(userId);
            if (current == cached) continue;
            presence.setCachedStatus(userId, current);
            broadcast(userId, current);
        }
    }

    private void broadcast(long targetUserId, Status status) {
        Set<Long> watchers = subs.watchersOf(targetUserId);
        PresenceUpdate update = new PresenceUpdate(targetUserId, status);
        for (Long watcherId : watchers) {
            stomp.convertAndSendToUser(String.valueOf(watcherId), "/queue/presence", update);
        }
    }
}
