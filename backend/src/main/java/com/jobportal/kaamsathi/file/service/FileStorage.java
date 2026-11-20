package com.jobportal.kaamsathi.file.service;

import org.springframework.core.io.Resource;

/**
 * WHERE file bytes are kept. Today: LocalFileStorage (a folder on disk). Later: a MinIO / S3 version.
 * The rest of the app only knows this interface, so switching storage changes ONE class.
 */
public interface FileStorage {

    /** Save bytes under this key (the key is made by us, never by the user). */
    void save(String key, byte[] content);

    /** Read the bytes back (for download). */
    Resource load(String key);

    /** Remove the bytes (no error if they are already gone). */
    void delete(String key);
}
