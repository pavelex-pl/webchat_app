package com.webchat.message;

import com.webchat.attachment.Attachment;
import com.webchat.attachment.AttachmentRepository;
import com.webchat.attachment.dto.AttachmentResponse;
import com.webchat.auth.UserLookup;
import com.webchat.message.dto.MessageResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class MessageAssembler {

    private final AttachmentRepository attachments;
    private final UserLookup lookup;

    public MessageAssembler(AttachmentRepository attachments, UserLookup lookup) {
        this.attachments = attachments;
        this.lookup = lookup;
    }

    public MessageResponse one(Message m) {
        String username = m.getAuthorId() == null ? null : lookup.usernameOr(m.getAuthorId(), null);
        List<Attachment> list = attachments.findByMessageId(m.getId());
        return MessageResponse.from(m, username, list.stream().map(AttachmentResponse::from).toList());
    }

    public List<MessageResponse> many(Collection<Message> list) {
        if (list.isEmpty()) return List.of();
        Set<Long> userIds = list.stream()
                .map(Message::getAuthorId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> names = lookup.usernames(userIds);
        Set<Long> messageIds = list.stream().map(Message::getId).collect(Collectors.toSet());
        Map<Long, List<AttachmentResponse>> attByMessage = new HashMap<>();
        for (Attachment a : attachments.findByMessageIdIn(messageIds)) {
            attByMessage.computeIfAbsent(a.getMessageId(), k -> new ArrayList<>())
                    .add(AttachmentResponse.from(a));
        }
        return list.stream()
                .map(m -> MessageResponse.from(
                        m,
                        m.getAuthorId() == null ? null : names.get(m.getAuthorId()),
                        attByMessage.getOrDefault(m.getId(), List.of())))
                .toList();
    }
}
