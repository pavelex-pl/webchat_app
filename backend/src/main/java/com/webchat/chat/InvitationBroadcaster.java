package com.webchat.chat;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class InvitationBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(InvitationBroadcaster.class);

    private final SimpMessagingTemplate stomp;

    public InvitationBroadcaster(SimpMessagingTemplate stomp) {
        this.stomp = stomp;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInvitation(InvitationEvent e) {
        Map<String, Object> note = Map.of(
                "type", "invitation",
                "kind", e.kind().name().toLowerCase(),
                "chatId", e.chatId(),
                "invitationId", e.invitationId());
        stomp.convertAndSend("/topic/user." + e.inviteeId() + ".notifications", note);
        log.info("notify invitation inviteeId={} chatId={} kind={}", e.inviteeId(), e.chatId(), e.kind());
    }
}
