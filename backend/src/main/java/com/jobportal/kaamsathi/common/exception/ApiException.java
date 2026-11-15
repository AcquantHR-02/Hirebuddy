package com.jobportal.kaamsathi.common.exception;

/**
 * Thrown by services when a business rule fails, e.g.
 *   throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS);
 * GlobalExceptionHandler turns it into ApiError JSON with the code's HTTP status.
 */
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;

    // use the code's default message
    public ApiException(ErrorCode errorCode) {
        this(errorCode, errorCode.getDefaultMessage());
    }

    // use a custom message (when the default is not specific enough)
    public ApiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
