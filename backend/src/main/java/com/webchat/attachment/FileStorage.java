package com.webchat.attachment;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FileStorage {

    private static final Logger log = LoggerFactory.getLogger(FileStorage.class);

    private final Path root;

    public FileStorage(@Value("${webchat.files.path:/data/files}") String rootPath) throws IOException {
        this.root = Path.of(rootPath).toAbsolutePath().normalize();
        Files.createDirectories(this.root);
    }

    public Stored store(Long chatId, String originalName, InputStream data, long expectedSize) throws IOException {
        String safe = sanitize(originalName);
        String relDir = "chat-" + chatId + "/" + UUID.randomUUID();
        Path dir = root.resolve(relDir);
        Files.createDirectories(dir);
        Path file = dir.resolve(safe);
        long size = Files.copy(data, file, StandardCopyOption.REPLACE_EXISTING);
        if (expectedSize > 0 && size != expectedSize) {
            log.warn("Stored size {} != multipart size {}", size, expectedSize);
        }
        return new Stored(relDir + "/" + safe, size);
    }

    public InputStream open(String storagePath) throws IOException {
        return Files.newInputStream(resolve(storagePath));
    }

    public long size(String storagePath) throws IOException {
        return Files.size(resolve(storagePath));
    }

    public void deleteFile(String storagePath) {
        try {
            Files.deleteIfExists(resolve(storagePath));
        } catch (IOException e) {
            log.warn("Failed to delete file {}: {}", storagePath, e.toString());
        }
    }

    public void deleteChatDir(Long chatId) {
        Path dir = root.resolve("chat-" + chatId).normalize();
        if (!dir.startsWith(root) || !Files.exists(dir)) return;
        try (var stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.deleteIfExists(p); }
                catch (IOException e) { log.warn("delete {}: {}", p, e.toString()); }
            });
        } catch (IOException e) {
            log.warn("Failed to walk {}: {}", dir, e.toString());
        }
    }

    private Path resolve(String storagePath) {
        Path p = root.resolve(storagePath).normalize();
        if (!p.startsWith(root)) throw new IllegalArgumentException("Invalid storage path");
        return p;
    }

    private static String sanitize(String name) {
        if (name == null || name.isBlank()) return "file";
        String base = name.replace('\\', '/');
        int slash = base.lastIndexOf('/');
        if (slash >= 0) base = base.substring(slash + 1);
        base = base.replaceAll("[^A-Za-z0-9._\\-]", "_");
        if (base.isBlank() || base.equals(".") || base.equals("..")) return "file";
        if (base.length() > 200) base = base.substring(base.length() - 200);
        return base;
    }

    public record Stored(String storagePath, long size) {}
}
