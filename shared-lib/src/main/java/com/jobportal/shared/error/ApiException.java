package com.jobportal.shared.error;

/**
 * Thrown by our code when a request can't be completed.
 * Carries what the error reply (ApiError) needs: HTTP status + short code.
 * Example: throw new ApiException(404, "NOT_FOUND", "Job not found");
 */
public class ApiException extends RuntimeException {

    private final int status;
    private final String code;

    public ApiException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public int getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
