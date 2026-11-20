package com.jobportal.kaamsathi.file.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Facts about one uploaded file = one row of `files` (V11). The bytes are in FileStorage under storageKey.
 * Never changed after upload (no setters, no behaviour methods).
 */
@Entity
@Table(name = "files")
public class StoredFile {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "owner_id", nullable = false, length = 36)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FileKind kind;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "storage_key", nullable = false, length = 255)
    private String storageKey;

    @Column(nullable = false, length = 64)
    private String sha256;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected StoredFile() {
    }

    public StoredFile(UUID id, UUID ownerId, FileKind kind, String originalName, String contentType,
                      long sizeBytes, String storageKey, String sha256, Instant now) {
        this.id = id;
        this.ownerId = ownerId;
        this.kind = kind;
        this.originalName = originalName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.storageKey = storageKey;
        this.sha256 = sha256;
        this.createdAt = now;
    }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public FileKind getKind() { return kind; }
    public String getOriginalName() { return originalName; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public String getStorageKey() { return storageKey; }
    public String getSha256() { return sha256; }
    public Instant getCreatedAt() { return createdAt; }
}
