package com.webchat.attachment;

import com.webchat.attachment.dto.AttachmentResponse;
import com.webchat.auth.CurrentUserResolver;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class AttachmentController {

    private final AttachmentService attachments;
    private final CurrentUserResolver currentUser;

    public AttachmentController(AttachmentService attachments, CurrentUserResolver currentUser) {
        this.attachments = attachments;
        this.currentUser = currentUser;
    }

    @PostMapping("/api/chats/{chatId}/attachments")
    public ResponseEntity<AttachmentResponse> upload(@PathVariable Long chatId,
                                                     @RequestParam("file") MultipartFile file) {
        Attachment a = attachments.upload(currentUser.require().userId(), chatId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(AttachmentResponse.from(a));
    }

    @GetMapping("/api/attachments/{id}")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long id) throws IOException {
        Attachment a = attachments.requireForDownload(currentUser.require().userId(), id);
        var stream = attachments.storage().open(a.getStoragePath());
        String encoded = URLEncoder.encode(a.getOriginalName(), StandardCharsets.UTF_8).replace("+", "%20");
        String dispositionValue = "attachment; filename*=UTF-8''" + encoded;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, dispositionValue)
                .contentType(MediaType.parseMediaType(a.getMimeType()))
                .contentLength(a.getSizeBytes())
                .body(new InputStreamResource(stream));
    }
}
