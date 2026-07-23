package dev.primer.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.stereotype.Component;

import dev.primer.config.StorageProperties;
import dev.primer.exception.PaperDownloadException;

@Component
public class PaperStorage {

    private final Path root;

    public PaperStorage(StorageProperties properties) {
        this.root = properties.getRoot().toAbsolutePath().normalize();
    }

    public Path temporaryPath(UUID id) {
        return root.resolve(".tmp").resolve(id + ".part");
    }

    public Path permanentPath(UUID id) {
        return root.resolve(id + ".pdf");
    }

    public void prepare(Path path) {
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException exception) {
            throw new PaperDownloadException("Could not prepare PDF storage.", exception);
        }
    }

    public void promote(Path temporary, Path permanent) {
        try {
            Files.createDirectories(permanent.getParent());
            Files.move(temporary, permanent, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new PaperDownloadException("Could not save the downloaded PDF.", exception);
        }
    }

    public boolean exists(String storagePath) {
        return Files.isRegularFile(Path.of(storagePath));
    }

    public Path resolve(String storagePath) {
        Path path = Path.of(storagePath).toAbsolutePath().normalize();
        if (!path.startsWith(root)) {
            throw new IllegalStateException("Paper storage path is outside the configured storage root.");
        }
        return path;
    }

    public void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // The original download error is more useful to callers than cleanup failure.
        }
    }
}
