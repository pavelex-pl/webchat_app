package com.webchat.message.dto;

import com.webchat.attachment.dto.AttachmentResponse;
import com.webchat.message.Message;
import java.time.Instant;
import java.util.List;

public record MessageResponse(
        Long id,
        Long chatId,
        Long authorId,
        String authorUsername,
        Long replyToId,
        String body,
        Instant createdAt,
        Instant editedAt,
        boolean deleted,
        List<AttachmentResponse> attachments
) {
    public static MessageResponse from(Message m, String authorUsername, List<AttachmentResponse> attachments) {
        return new MessageResponse(
                m.getId(),
                m.getChatId(),
                m.getAuthorId(),
                authorUsername,
                m.getReplyToId(),
                m.isDeleted() ? null : m.getBody(),
                m.getCreatedAt(),
                m.getEditedAt(),
                m.isDeleted(),
                attachments == null ? List.of() : attachments
        );
    }
}
