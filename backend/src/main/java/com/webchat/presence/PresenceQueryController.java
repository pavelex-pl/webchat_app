package com.webchat.presence;

import com.webchat.presence.dto.PresenceUpdate;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Synchronous lookup of current presence statuses. Used by the client on page
 * load / reconnect to seed the presence store reliably — avoiding the STOMP
 * SUBSCRIBE / inbound-reply race that leaks initial statuses on some tab
 * refreshes.
 */
@RestController
@RequestMapping("/api/presence")
public class PresenceQueryController {

    private final PresenceService presence;

    public PresenceQueryController(PresenceService presence) {
        this.presence = presence;
    }

    @GetMapping
    public List<PresenceUpdate> query(@RequestParam("userIds") List<Long> userIds) {
        if (userIds == null) return List.of();
        return userIds.stream()
                .map(id -> new PresenceUpdate(id, presence.compute(id)))
                .toList();
    }
}
