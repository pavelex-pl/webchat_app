package com.webchat.presence;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class PresenceSessionListener {

    private final PresenceService presence;
    private final PresenceSubscriptions subs;

    public PresenceSessionListener(PresenceService presence, PresenceSubscriptions subs) {
        this.presence = presence;
        this.subs = subs;
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        String sessionId = StompHeaderAccessor.wrap(event.getMessage()).getSessionId();
        if (sessionId == null) return;
        PresenceSubscriptions.DisconnectInfo info = subs.removeSession(sessionId);
        if (info.userId() == 0L) return;
        for (String tabId : info.tabs()) {
            presence.removeTab(info.userId(), tabId);
        }
    }
}
