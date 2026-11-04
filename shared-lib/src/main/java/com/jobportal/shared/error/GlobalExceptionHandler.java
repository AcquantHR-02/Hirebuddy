package com.jobportal.shared.error;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Clock;
import java.time.Instant;

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
}
