package com.webchat.message;

import com.webchat.auth.CurrentUserResolver;
import com.webchat.message.dto.MessageResponse;
import com.webchat.message.dto.ReadMarkerRequest;
import com.webchat.message.dto.SendMessageRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chats/{chatId}")
public class ChatMessagesController {

    private final MessageService messages;
    private final MessageAssembler assembler;
    private final CurrentUserResolver currentUser;

    public ChatMessagesController(MessageService messages, MessageAssembler assembler,
                                  CurrentUserResolver currentUser) {
        this.messages = messages;
        this.assembler = assembler;
        this.currentUser = currentUser;
    }

    @GetMapping("/messages")
    public List<MessageResponse> list(@PathVariable Long chatId,
                                      @RequestParam(required = false) Long before,
                                      @RequestParam(defaultValue = "50") int limit) {
        Long uid = currentUser.require().userId();
        return assembler.many(messages.list(uid, chatId, before, limit));
    }

    @PostMapping("/messages")
    public ResponseEntity<MessageResponse> send(@PathVariable Long chatId,
                                                @Valid @RequestBody SendMessageRequest req) {
        Long uid = currentUser.require().userId();
        Message m = messages.send(uid, chatId, req.body(), req.replyToId(), req.attachments());
        return ResponseEntity.status(HttpStatus.CREATED).body(assembler.one(m));
    }

    @PostMapping("/read")
    public ResponseEntity<Void> read(@PathVariable Long chatId, @Valid @RequestBody ReadMarkerRequest req) {
        messages.markRead(currentUser.require().userId(), chatId, req.lastReadMessageId());
        return ResponseEntity.noContent().build();
    }
}
