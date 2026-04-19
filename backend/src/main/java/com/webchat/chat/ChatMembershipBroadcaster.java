package com.webchat.chat;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ChatMembershipBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(ChatMembershipBroadcaster.class);

    private final SimpMessagingTemplate stomp;

    public ChatMembershipBroadcaster(SimpMessagingTemplate stomp) {
        this.stomp = stomp;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMembershipChange(ChatMembershipEvent e) {
        Map<String, Object> payload = Map.of(
                "kind", e.kind().name().toLowerCase(),
                "userId", e.userId());
        stomp.convertAndSend("/topic/chat." + e.chatId() + ".members", payload);
        log.info("notify membership chatId={} userId={} kind={}", e.chatId(), e.userId(), e.kind());
    }
}
