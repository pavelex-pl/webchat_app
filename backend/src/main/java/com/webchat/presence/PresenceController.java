package com.webchat.presence;

import com.webchat.presence.dto.PingRequest;
import com.webchat.presence.dto.PresenceUpdate;
import com.webchat.presence.dto.WatchRequest;
import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
public class PresenceController {

    private final PresenceService presence;
    private final PresenceSubscriptions subs;
    private final SimpMessagingTemplate stomp;

    public PresenceController(PresenceService presence, PresenceSubscriptions subs, SimpMessagingTemplate stomp) {
        this.presence = presence;
        this.subs = subs;
        this.stomp = stomp;
    }

    @MessageMapping("/presence/ping")
    public void ping(@Payload PingRequest req, StompHeaderAccessor hdr, Principal principal) {
        if (principal == null || req == null || req.tabId() == null) return;
        long userId = Long.parseLong(principal.getName());
        subs.registerTab(hdr.getSessionId(), userId, req.tabId());
        long ts = req.lastActivityAt() > 0 ? req.lastActivityAt() : System.currentTimeMillis();
        presence.recordActivity(userId, req.tabId(), ts);
    }

    @MessageMapping("/watch")
    @SendToUser("/queue/presence")
    public List<PresenceUpdate> watch(@Payload WatchRequest req, StompHeaderAccessor hdr, Principal principal) {
        if (principal == null || req == null || req.userIds() == null) return List.of();
        long watcherId = Long.parseLong(principal.getName());
        Set<Long> targets = new HashSet<>(req.userIds());
        subs.addWatches(hdr.getSessionId(), watcherId, targets);
        return targets.stream()
                .map(t -> new PresenceUpdate(t, presence.compute(t)))
                .toList();
    }

    @MessageMapping("/unwatch")
    public void unwatch(@Payload WatchRequest req, StompHeaderAccessor hdr, Principal principal) {
        if (principal == null || req == null || req.userIds() == null) return;
        long watcherId = Long.parseLong(principal.getName());
        subs.removeWatches(hdr.getSessionId(), watcherId, new HashSet<>(req.userIds()));
    }

    public SimpMessagingTemplate stomp() { return stomp; }
}
