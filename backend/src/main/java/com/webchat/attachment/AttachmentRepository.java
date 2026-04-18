package com.webchat.attachment;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByMessageIdIn(Collection<Long> messageIds);
    List<Attachment> findByMessageId(Long messageId);
}
