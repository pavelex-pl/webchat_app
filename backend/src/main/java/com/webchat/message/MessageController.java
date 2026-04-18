package com.webchat.message;

import com.webchat.auth.CurrentUserResolver;
import com.webchat.message.dto.EditMessageRequest;
import com.webchat.message.dto.MessageResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messages;
    private final MessageAssembler assembler;
    private final CurrentUserResolver currentUser;

    public MessageController(MessageService messages, MessageAssembler assembler,
                             CurrentUserResolver currentUser) {
        this.messages = messages;
        this.assembler = assembler;
        this.currentUser = currentUser;
    }

    @PatchMapping("/{id}")
    public MessageResponse edit(@PathVariable Long id, @Valid @RequestBody EditMessageRequest req) {
        Long uid = currentUser.require().userId();
        Message m = messages.edit(uid, id, req.body());
        return assembler.one(m);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        messages.delete(currentUser.require().userId(), id);
        return ResponseEntity.noContent().build();
    }
}
