package com.webchat.friends;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class FriendRequestBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(FriendRequestBroadcaster.class);

    private final SimpMessagingTemplate stomp;

    public FriendRequestBroadcaster(SimpMessagingTemplate stomp) {
        this.stomp = stomp;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFriendRequest(FriendRequestEvent e) {
        Map<String, Object> note = Map.of(
                "type", "friend_request",
                "kind", e.kind().name().toLowerCase(),
                "otherUserId", e.otherUserId());
        stomp.convertAndSend("/topic/user." + e.recipientId() + ".notifications", note);
        log.info("notify friend_request recipientId={} otherUserId={} kind={}",
                e.recipientId(), e.otherUserId(), e.kind());
    }
}
