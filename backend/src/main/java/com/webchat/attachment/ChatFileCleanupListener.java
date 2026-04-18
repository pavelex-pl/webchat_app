package com.webchat.attachment;

import com.webchat.chat.ChatDeletedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ChatFileCleanupListener {

    private final FileStorage storage;

    public ChatFileCleanupListener(FileStorage storage) {
        this.storage = storage;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ChatDeletedEvent e) {
        storage.deleteChatDir(e.chatId());
    }
}
