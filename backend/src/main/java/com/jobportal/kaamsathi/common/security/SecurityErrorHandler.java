package com.jobportal.kaamsathi.common.security;

import com.jobportal.kaamsathi.common.dto.ApiError;
import com.jobportal.kaamsathi.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;

/**
 * Security errors happen in FILTERS, before any controller, so GlobalExceptionHandler never sees them.
 * This class writes the same ApiError JSON for them:
 *   401 INVALID_TOKEN  - no token, broken token, expired token
 *   403 ACCESS_DENIED  - valid token, but the role is not allowed
 */
@Component
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final JsonMapper jsonMapper;   // Spring Boot's JSON tool (same format as controllers)
    private final Clock clock;

    public SecurityErrorHandler(JsonMapper jsonMapper, Clock clock) {
        this.jsonMapper = jsonMapper;
        this.clock = clock;
    }

    // "who are you?" → 401
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        write(response, request, ErrorCode.INVALID_TOKEN);
    }

    // "I know you, but no" → 403
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        write(response, request, ErrorCode.ACCESS_DENIED);
    }

    private void write(HttpServletResponse response, HttpServletRequest request, ErrorCode code)
            throws IOException {
        ApiError body = new ApiError(
                Instant.now(clock),
                code.getStatus().value(),
                code.name(),
                code.getDefaultMessage(),
                request.getRequestURI());
        response.setStatus(code.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(), body);
    }
}
