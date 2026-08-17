package com.example.pizzaconfigurator.catalog.infrastructure.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Local-disk storage for admin-uploaded pizza photos, one file per pizza
 * ({pizzaId}.{ext} — a re-upload replaces whatever extension was there
 * before). The configured directory must be a persistent volume in any
 * real deployment (see compose*.yaml's pizza-images volume) since a
 * container's own filesystem is otherwise wiped on every redeploy.
 */
@Component
public class PizzaImageStorage {

    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
        "image/jpeg", "jpg",
        "image/png", "png",
        "image/webp", "webp"
    );

    private final Path directory;

    PizzaImageStorage(@Value("${app.storage.pizza-images-dir}") String directory) {
        this.directory = Path.of(directory);
        try {
            Files.createDirectories(this.directory);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create pizza image storage directory: " + this.directory, e);
        }
    }

    public boolean supports(String contentType) {
        return EXTENSION_BY_CONTENT_TYPE.containsKey(contentType);
    }

    public void store(UUID pizzaId, String contentType, byte[] bytes) {
        String extension = EXTENSION_BY_CONTENT_TYPE.get(contentType);
        if (extension == null) {
            throw new IllegalArgumentException("Unsupported image content type: " + contentType);
        }
        deleteExisting(pizzaId);
        try {
            Files.write(directory.resolve(pizzaId + "." + extension), bytes);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store pizza image for " + pizzaId, e);
        }
    }

    public Optional<StoredImage> load(UUID pizzaId) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, pizzaId + ".*")) {
            for (Path path : stream) {
                String extension = fileExtension(path);
                String contentType = EXTENSION_BY_CONTENT_TYPE.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(extension))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse("application/octet-stream");
                return Optional.of(new StoredImage(Files.readAllBytes(path), contentType));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read pizza image for " + pizzaId, e);
        }
        return Optional.empty();
    }

    private void deleteExisting(UUID pizzaId) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, pizzaId + ".*")) {
            for (Path path : stream) {
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not clear previous pizza image for " + pizzaId, e);
        }
    }

    private static String fileExtension(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1);
    }

    public record StoredImage(byte[] bytes, String contentType) {
    }
}
