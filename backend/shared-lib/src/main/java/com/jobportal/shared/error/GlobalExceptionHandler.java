package com.jobportal.shared.error;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Catches exceptions thrown anywhere in a service's controllers
 * and turns them into ApiError JSON replies.
 * Services switch it on with @Import(GlobalExceptionHandler.class).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final Clock clock;

    // Spring calls this and passes in the Clock (dependency injection)
    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    // Runs whenever an ApiException escapes from a controller
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException ex, HttpServletRequest request) {
        ApiError body = new ApiError(
                Instant.now(clock),
                ex.getStatus(),
                ex.getCode(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    // Runs when no controller matches the URL (Spring throws NoResourceFoundException).
    // Without this, Spring would send its own error shape instead of our ApiError.
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoEndpoint(NoResourceFoundException ex, HttpServletRequest request) {
        ApiError body = new ApiError(
                Instant.now(clock),
                404,
                "NOT_FOUND",
                "No endpoint " + request.getMethod() + " " + request.getRequestURI(),
                request.getRequestURI()
        );
        return ResponseEntity.status(404).body(body);
    }

    // Runs when @Valid finds broken rules in a request body (bad email, short password...).
    // All broken rules go into one message, sorted by field so the text is always the same.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField))
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));

        ApiError body = new ApiError(
                Instant.now(clock),
                400,
                "VALIDATION_FAILED",
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(400).body(body);
    }
}
