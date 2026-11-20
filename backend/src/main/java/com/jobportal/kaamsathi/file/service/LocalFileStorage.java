package com.jobportal.kaamsathi.file.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * FileStorage on the local disk: every file is <app.files.dir>/<key>, e.g. uploads/resume/2b6f...
 * Dev only; production will use MinIO (same interface).
 */
@Component
public class LocalFileStorage implements FileStorage {

    private final Path root;

    public LocalFileStorage(@Value("${app.files.dir}") String dir) {
        this.root = Path.of(dir).toAbsolutePath().normalize();
    }

    @Override
    public void save(String key, byte[] content) {
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            // CREATE_NEW: never overwrite an existing file (keys are unique UUIDs anyway)
            Files.write(target, content, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save file " + key, e);
        }
    }

    @Override
    public Resource load(String key) {
        return new PathResource(resolve(key));
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not delete file " + key, e);
        }
    }

    // key → path INSIDE root. "../" tricks can't escape the folder (path traversal guard).
    private Path resolve(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Bad storage key");
        }
        return path;
    }
}
