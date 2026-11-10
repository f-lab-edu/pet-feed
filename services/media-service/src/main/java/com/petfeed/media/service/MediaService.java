package com.petfeed.media.service;

import com.petfeed.media.domain.MediaFile;
import com.petfeed.media.repository.MediaRepository;
import com.petfeed.domain.dto.response.MediaResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
public class MediaService {
    private final MediaRepository files;
    private final Path root;

    public MediaService(MediaRepository files, @Value("${media.storage-path}") String root) throws IOException {
        this.files = files;
        this.root = Path.of(root).toAbsolutePath().normalize();
        Files.createDirectories(this.root);
    }

    public MediaResponse upload(String owner, String contentType, byte[] body) throws IOException {
        if (owner == null || owner.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        String actualType = MediaValidator.validate(body, contentType);
        String id = UUID.randomUUID().toString();
        Path target = root.resolve(id);
        try {
            Files.write(target, body, StandardOpenOption.CREATE_NEW);
            // Repository commit finishes before returning; a failed commit removes the file.
            var saved = files.saveAndFlush(new MediaFile(id, owner, actualType, body.length));
            return describe(saved);
        } catch (IOException | RuntimeException failure) {
            try { Files.deleteIfExists(target); } catch (IOException cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }

    public List<MediaResponse> validateOwnership(String owner, List<String> ids) {
        if (ids == null || ids.size() > 6 || new HashSet<>(ids).size() != ids.size())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return ids.stream().map(this::get).map(file -> {
            if (!file.getOwnerKey().equals(owner)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            return describe(file);
        }).toList();
    }

    public MediaFile get(String id) {
        if (id == null || !id.matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}"))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return files.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public FileSystemResource resource(MediaFile file) {
        Path path = root.resolve(file.getId());
        if (!Files.isRegularFile(path)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return new FileSystemResource(path);
    }

    private MediaResponse describe(MediaFile file) {
        return new MediaResponse(file.getId(), file.getContentType(), file.getSize());
    }
}
