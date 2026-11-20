package com.jobportal.kaamsathi.file.service;

import com.jobportal.kaamsathi.common.exception.ApiException;
import com.jobportal.kaamsathi.common.exception.ErrorCode;
import com.jobportal.kaamsathi.common.util.SecureTokens;
import com.jobportal.kaamsathi.file.dto.FileContent;
import com.jobportal.kaamsathi.file.dto.FileResponse;
import com.jobportal.kaamsathi.file.entity.DetectedType;
import com.jobportal.kaamsathi.file.entity.FileKind;
import com.jobportal.kaamsathi.file.entity.StoredFile;
import com.jobportal.kaamsathi.file.repository.StoredFileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.util.Locale;
import java.util.UUID;

/**
 * Upload / read files. Other modules (seeker, auth, application…) call this; they decide WHO may see a file,
 * this class only stores, checks and returns it.
 */
@Service
public class FileService {

    private final StoredFileRepository fileRepository;
    private final FileStorage storage;
    private final Clock clock;

    public FileService(StoredFileRepository fileRepository, FileStorage storage, Clock clock) {
        this.fileRepository = fileRepository;
        this.storage = storage;
        this.clock = clock;
    }

    /**
     * Check + save one upload. Checks: not empty (400), size for this kind (413),
     * real type from the first bytes is allowed for this kind (415).
     * Bytes go to storage first, then the DB row; if the row fails, the bytes are removed again.
     * If the surrounding transaction rolls back later, the bytes are removed after the rollback.
     */
    @Transactional
    public FileResponse upload(UUID ownerId, FileKind kind, MultipartFile upload) {
        if (upload == null || upload.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "file: file is empty or missing");
        }
        if (upload.getSize() > kind.maxBytes()) {
            throw new ApiException(ErrorCode.FILE_TOO_LARGE,
                    "File is too large (max " + kind.maxBytes() / (1024 * 1024) + " MB)");
        }

        byte[] bytes = readAll(upload);
        String name = cleanName(upload.getOriginalFilename());
        DetectedType type = DetectedType.detect(bytes, name);
        if (type == null || !kind.allowed().contains(type)) {
            throw new ApiException(ErrorCode.FILE_TYPE_NOT_ALLOWED,
                    "Allowed types: " + kind.allowed().toString().toLowerCase(Locale.ROOT));
        }

        UUID id = UUID.randomUUID();
        String key = kind.name().toLowerCase(Locale.ROOT) + "/" + id;      // our key, never the user's file name
        storage.save(key, bytes);
        try {
            StoredFile file = fileRepository.saveAndFlush(new StoredFile(
                    id, ownerId, kind, name, type.contentType(), bytes.length,
                    key, SecureTokens.sha256Hex(bytes), clock.instant()));
            // the CALLER's transaction may still fail later (e.g. payment race → 409): then the row
            // is rolled back, so remove the bytes too (disk is not part of the DB transaction)
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        storage.delete(key);
                    }
                }
            });
            return FileResponse.from(file);
        } catch (RuntimeException e) {
            storage.delete(key);    // no row → no orphan bytes on disk
            throw e;
        }
    }

    /** Facts about a file (no bytes). Unknown → 404. */
    @Transactional(readOnly = true)
    public FileResponse get(UUID fileId) {
        return FileResponse.from(find(fileId));
    }

    /** The bytes + type + name, for a download. The CALLER must check access first. Unknown → 404. */
    @Transactional(readOnly = true)
    public FileContent open(UUID fileId) {
        StoredFile file = find(fileId);
        return new FileContent(storage.load(file.getStorageKey()), file.getContentType(),
                file.getOriginalName(), file.getSizeBytes());
    }

    /**
     * Remove a file for good (e.g. an old profile picture). The CALLER must check it is allowed.
     * Row goes now; bytes go only AFTER the transaction commits — if the tx rolls back, the row
     * comes back and its bytes are still there (never a row pointing at missing bytes).
     * Unknown id → nothing happens.
     */
    @Transactional
    public void delete(UUID fileId) {
        fileRepository.findById(fileId).ifPresent(file -> {
            fileRepository.delete(file);
            String key = file.getStorageKey();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    storage.delete(key);
                }
            });
        });
    }

    private StoredFile find(UUID fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new ApiException(ErrorCode.FILE_NOT_FOUND));
    }

    private static byte[] readAll(MultipartFile upload) {
        try {
            return upload.getBytes();          // max 5 MB, fine in memory
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read upload", e);
        }
    }

    // keep only the plain file name ("C:\fake\path\cv.pdf" → "cv.pdf"), drop control characters, max 255
    static String cleanName(String raw) {
        if (raw == null || raw.isBlank()) {
            return "file";
        }
        String name = raw.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("\\p{Cntrl}", "").trim();
        if (name.isEmpty()) {
            return "file";
        }
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }
}
