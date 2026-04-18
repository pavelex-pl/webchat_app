package com.webchat.message;

import com.webchat.chat.ChatMember;
import com.webchat.chat.ChatMemberRepository;
import com.webchat.message.dto.MessageResponse;
import java.util.Map;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class MessageBroadcaster {

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

    private void publish(String kind, Message m) {
        MessageResponse dto = assembler.one(m);
        stomp.convertAndSend("/topic/chat." + m.getChatId(), Map.of("kind", kind, "message", dto));
    }

    private void notifyMembers(Message m) {
        Map<String, Object> note = Map.of("type", "message", "chatId", m.getChatId(), "messageId", m.getId());
        for (ChatMember cm : members.findByIdChatId(m.getChatId())) {
            if (m.getAuthorId() != null && cm.getUserId().equals(m.getAuthorId())) continue;
            stomp.convertAndSendToUser(String.valueOf(cm.getUserId()), "/queue/notifications", note);
        }
    }
}
