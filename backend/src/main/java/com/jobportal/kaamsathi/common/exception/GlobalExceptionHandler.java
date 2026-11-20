package com.jobportal.kaamsathi.common.exception;

import java.time.Clock;
import java.time.Instant;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.jobportal.kaamsathi.common.dto.ApiError;

/**
 * Catches errors from ALL controllers and turns them into one ApiError JSON.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    /** Unknown URL → 404 */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(HttpServletRequest request) {
        return build(ErrorCode.NOT_FOUND, request);
    }

    /** Right URL, wrong method (e.g. GET on a POST endpoint) → 405 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotAllowed(HttpServletRequest request) {
        return build(ErrorCode.METHOD_NOT_ALLOWED, request);
    }

    /** Body missing or broken JSON → 400 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleMalformedJson(HttpServletRequest request) {
        return build(ErrorCode.MALFORMED_JSON, request);
    }

    /**
     * Request body failed {@code @Valid} checks (e.g. bad email) → 400.
     * Message lists every bad field, sorted so it is always the same order:
     * "email: email is not valid; password: password must be 8 to 72 characters"
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                     HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return build(ErrorCode.VALIDATION_FAILED, message, request);
    }

    /** Business rule failed in a service (e.g. EMAIL_ALREADY_EXISTS) → the code's own status */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException ex, HttpServletRequest request) {
        return build(ex.getErrorCode(), ex.getMessage(), request);
    }

    /**
     * {@code @PreAuthorize} said no (wrong role) → 403. Needed here: this error is thrown INSIDE the
     * controller call, so without this the catch-all below would turn it into a 500.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(ErrorCode.ACCESS_DENIED, request);
    }

    /** URL part has the wrong type, e.g. /users/abc/suspend where a UUID is expected → 400 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                       HttpServletRequest request) {
        return build(ErrorCode.VALIDATION_FAILED, ex.getName() + ": is not valid", request);
    }

    /** Upload bigger than spring.servlet.multipart limits → 413 (Spring stops it before our code runs). */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleUploadTooLarge(MaxUploadSizeExceededException ex,
                                                         HttpServletRequest request) {
        return build(ErrorCode.FILE_TOO_LARGE, "File is too large (max 5 MB)", request);
    }

    /** Multipart request without the expected "file" part → 400. */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiError> handleMissingPart(MissingServletRequestPartException ex,
                                                      HttpServletRequest request) {
        return build(ErrorCode.VALIDATION_FAILED, ex.getRequestPartName() + ": is required", request);
    }

    /**
     * Upload endpoint called without multipart/form-data (e.g. JSON body) → 400.
     * (MaxUploadSizeExceededException is a sub-type, but its own handler above is more specific, so it wins.)
     */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiError> handleNotMultipart(MultipartException ex, HttpServletRequest request) {
        return build(ErrorCode.VALIDATION_FAILED, "file: send the file as multipart/form-data", request);
    }

    /** Anything else (a bug) → 500. Log the full error, but never send details to the client. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(ErrorCode.INTERNAL_ERROR, request);
    }

    private ResponseEntity<ApiError> build(ErrorCode code, HttpServletRequest request) {
        return build(code, code.getDefaultMessage(), request);
    }

    // same as above, but with our own message instead of the default one
    private ResponseEntity<ApiError> build(ErrorCode code, String message, HttpServletRequest request) {
        ApiError body = new ApiError(
                Instant.now(clock),
                code.getStatus().value(),
                code.name(),
                message,
                request.getRequestURI());
        return ResponseEntity.status(code.getStatus()).body(body);
    }
}
