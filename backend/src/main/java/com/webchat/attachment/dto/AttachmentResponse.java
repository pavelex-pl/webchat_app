package com.webchat.attachment.dto;

import com.webchat.attachment.Attachment;

public record AttachmentResponse(
        Long id,
        String originalName,
        String mimeType,
        long sizeBytes,
        String comment
) {
    public static AttachmentResponse from(Attachment a) {
        return new AttachmentResponse(a.getId(), a.getOriginalName(), a.getMimeType(), a.getSizeBytes(), a.getComment());
    }
}
