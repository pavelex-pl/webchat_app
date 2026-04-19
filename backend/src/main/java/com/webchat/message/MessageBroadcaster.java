package com.webchat.message;

import com.webchat.chat.ChatMember;
import com.webchat.chat.ChatMemberRepository;
import com.webchat.message.dto.MessageResponse;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class MessageBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(MessageBroadcaster.class);

    private final SimpMessagingTemplate stomp;
    private final MessageAssembler assembler;
    private final ChatMemberRepository members;

    public MessageBroadcaster(SimpMessagingTemplate stomp, MessageAssembler assembler,
                              ChatMemberRepository members) {
        this.stomp = stomp;
        this.assembler = assembler;
        this.members = members;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCreated(MessageCreatedEvent e) {
        publish("created", e.message());
        notifyMembers(e.message());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUpdated(MessageUpdatedEvent e) {
        publish(e.message().isDeleted() ? "deleted" : "updated", e.message());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRead(MessageReadEvent e) {
        Map<String, Object> note = Map.of(
                "type", "read",
                "chatId", e.chatId(),
                "lastReadMessageId", e.lastReadMessageId());
        stomp.convertAndSend("/topic/user." + e.userId() + ".notifications", note);
        log.info("notify read userId={} chatId={} lastReadMessageId={}",
                e.userId(), e.chatId(), e.lastReadMessageId());
    }

    private void publish(String kind, Message m) {
        MessageResponse dto = assembler.one(m);
        stomp.convertAndSend("/topic/chat." + m.getChatId(), Map.of("kind", kind, "message", dto));
    }

    private void notifyMembers(Message m) {
        Map<String, Object> note = Map.of("type", "message", "chatId", m.getChatId(), "messageId", m.getId());
        int count = 0;
        for (ChatMember cm : members.findByIdChatId(m.getChatId())) {
            if (m.getAuthorId() != null && cm.getUserId().equals(m.getAuthorId())) continue;
            stomp.convertAndSend("/topic/user." + cm.getUserId() + ".notifications", note);
            count++;
        }
        log.info("notify message chatId={} messageId={} author={} recipients={}",
                m.getChatId(), m.getId(), m.getAuthorId(), count);
    }
}
