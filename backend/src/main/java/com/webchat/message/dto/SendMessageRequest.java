package com.webchat.message.dto;

import jakarta.validation.constraints.Size;
import java.util.List;

public record SendMessageRequest(
        @Size(max = 3072, message = "Message text exceeds 3 KB") String body,
        Long replyToId,
        List<AttachmentLink> attachments
) {
    public record AttachmentLink(Long id, String comment) {}
}
