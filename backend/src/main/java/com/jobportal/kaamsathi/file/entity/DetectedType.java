package com.jobportal.kaamsathi.file.entity;

/**
 * A file type WE recognised from the file's first bytes ("magic number"), with the content type we store.
 * The browser's Content-Type and the file name can lie; the first bytes can't (a renamed .exe is not "%PDF").
 */
public enum DetectedType {

    JPEG("image/jpeg"),
    PNG("image/png"),
    WEBP("image/webp"),
    PDF("application/pdf"),
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final String contentType;

    DetectedType(String contentType) {
        this.contentType = contentType;
    }

    public String contentType() { return contentType; }

    /** First bytes → type, or null if we don't know it. */
    public static DetectedType detect(byte[] b, String originalName) {
        if (startsWith(b, 0xFF, 0xD8, 0xFF)) {
            return JPEG;
        }
        if (startsWith(b, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) {
            return PNG;
        }
        if (startsWith(b, 'R', 'I', 'F', 'F') && b.length >= 12
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return WEBP;
        }
        if (startsWith(b, '%', 'P', 'D', 'F', '-')) {
            return PDF;
        }
        // DOCX is a ZIP file ("PK\3\4"). Other ZIPs exist too, so the name must also end in .docx
        if (startsWith(b, 'P', 'K', 0x03, 0x04) && originalName.toLowerCase().endsWith(".docx")) {
            return DOCX;
        }
        return null;
    }

    private static boolean startsWith(byte[] data, int... expected) {
        if (data.length < expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((data[i] & 0xFF) != expected[i]) {
                return false;
            }
        }
        return true;
    }
}
