package com.webchat.attachment;

import com.webchat.chat.ChatMemberRepository;
import com.webchat.chat.ChatPolicy;
import com.webchat.common.BadRequestException;
import com.webchat.common.NotFoundException;
import com.webchat.common.UnauthorizedException;
import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AttachmentService {

    private static final long MAX_FILE = 20L * 1024 * 1024;
    private static final long MAX_IMAGE = 3L * 1024 * 1024;

    private final AttachmentRepository attachments;
    private final ChatMemberRepository members;
    private final ChatPolicy policy;
    private final FileStorage storage;

    public AttachmentService(AttachmentRepository attachments, ChatMemberRepository members,
                             ChatPolicy policy, FileStorage storage) {
        this.attachments = attachments;
        this.members = members;
        this.policy = policy;
        this.storage = storage;
    }

    @Transactional
    public Attachment upload(Long userId, Long chatId, MultipartFile file) {
        policy.requireMembership(chatId, userId);
        if (file == null || file.isEmpty()) throw new BadRequestException("File required");
        long size = file.getSize();
        String mime = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        boolean isImage = mime.startsWith("image/");
        if (isImage && size > MAX_IMAGE) throw new BadRequestException("Image exceeds 3 MB");
        if (size > MAX_FILE) throw new BadRequestException("File exceeds 20 MB");
        FileStorage.Stored stored;
        try {
            stored = storage.store(chatId, file.getOriginalFilename(), file.getInputStream(), size);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
        Attachment a = new Attachment(chatId, userId,
                file.getOriginalFilename() == null ? "file" : file.getOriginalFilename(),
                stored.storagePath(), mime, stored.size());
        return attachments.save(a);
    }

    /**
     * Links orphan attachments to a freshly-created message. Validates each attachment:
     *   - exists
     *   - belongs to the same chat
     *   - uploader matches the caller
     *   - not yet linked to another message
     */
    @Transactional
    public void linkToMessage(Long userId, Long chatId, Long messageId, List<Long> attachmentIds,
                              java.util.Map<Long, String> commentsById) {
        if (attachmentIds == null || attachmentIds.isEmpty()) return;
        List<Attachment> list = attachments.findAllById(attachmentIds);
        if (list.size() != attachmentIds.size()) throw new BadRequestException("Unknown attachment id");
        for (Attachment a : list) {
            if (!chatId.equals(a.getChatId())) throw new BadRequestException("Attachment not in this chat");
            if (!userId.equals(a.getUploaderId())) throw new UnauthorizedException("Not your attachment");
            if (a.getMessageId() != null) throw new BadRequestException("Attachment already linked");
            a.setMessageId(messageId);
            if (commentsById != null) {
                String c = commentsById.get(a.getId());
                if (c != null) a.setComment(c.isBlank() ? null : c);
            }
        }
    }

    public Attachment requireForDownload(Long userId, Long attachmentId) {
        Attachment a = attachments.findById(attachmentId)
                .orElseThrow(() -> new NotFoundException("Attachment not found"));
        // spec §2.6.4 — must be a current member of the chat
        if (!members.existsByIdChatIdAndIdUserId(a.getChatId(), userId)) {
            throw new UnauthorizedException("No access to this attachment");
        }
        return a;
    }

    public FileStorage storage() { return storage; }
}
