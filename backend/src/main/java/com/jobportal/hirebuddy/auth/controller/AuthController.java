package com.jobportal.hirebuddy.auth.controller;

import com.jobportal.hirebuddy.auth.dto.DeleteAccountRequest;
import com.jobportal.hirebuddy.auth.dto.ForgotPasswordRequest;
import com.jobportal.hirebuddy.auth.dto.LoginRequest;
import com.jobportal.hirebuddy.auth.dto.RefreshRequest;
import com.jobportal.hirebuddy.auth.dto.ResetPasswordRequest;
import com.jobportal.hirebuddy.auth.dto.TokenResponse;
import com.jobportal.hirebuddy.auth.dto.UserResponse;
import com.jobportal.hirebuddy.auth.dto.VerifyEmailRequest;
import com.jobportal.hirebuddy.auth.service.AccountService;
import com.jobportal.hirebuddy.auth.service.LoginService;
import com.jobportal.hirebuddy.auth.service.TokenService;
import com.jobportal.hirebuddy.auth.service.UserService;
import com.jobportal.hirebuddy.auth.service.VerificationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

/**
 * Login, refresh, logout, verify-email, forgot/reset password, me, delete my account. HTTP only; rules live in the services.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String DEV_RESET_TOKEN_HEADER = "X-Dev-Reset-Token";

    private final LoginService loginService;
    private final TokenService tokenService;
    private final VerificationService verificationService;
    private final UserService userService;
    private final AccountService accountService;
    private final boolean exposeTokens;

    public AuthController(LoginService loginService, TokenService tokenService,
                          VerificationService verificationService, UserService userService,
                          AccountService accountService,
                          @Value("${app.dev.expose-tokens}") boolean exposeTokens) {
        this.loginService = loginService;
        this.tokenService = tokenService;
        this.verificationService = verificationService;
        this.userService = userService;
        this.accountService = accountService;
        this.exposeTokens = exposeTokens;
    }

    /**
     * POST /api/auth/forgot-password → ALWAYS 204, whether or not the email exists (public).
     * If it exists, a reset link is "emailed" (console for now). Dev header only when the dev setting is on.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        Optional<String> token = verificationService.sendPasswordReset(request.email());
        ResponseEntity.HeadersBuilder<?> reply = ResponseEntity.noContent();
        if (exposeTokens && token.isPresent()) {
            reply.header(DEV_RESET_TOKEN_HEADER, token.get());
        }
        return reply.build();
    }

    /** POST /api/auth/reset-password → 204; new password saved, every device logged out (public: the token is the proof) */
    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        verificationService.resetPassword(request.token(), request.newPassword());
    }

    /** POST /api/auth/verify-email → 204; account becomes ACTIVE (public: the token is the proof) */
    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        verificationService.verifyEmail(request.token());
    }

    /** POST /api/auth/login → 200 + access token (public) */
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return loginService.login(request);
    }

    /** POST /api/auth/refresh → 200 + NEW access + refresh token; the old refresh token is dead (public) */
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return tokenService.refresh(request.refreshToken());
    }

    /** POST /api/auth/logout → 204 No Content, always (public; the refresh token in the body is the proof) */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest request) {
        tokenService.logout(request.refreshToken());
    }

    /**
     * GET /api/auth/me → who am I (needs "Authorization: Bearer <token>").
     * Spring already checked the token; @AuthenticationPrincipal hands us the decoded JWT.
     */
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());   // "sub" claim = user id
        return userService.getById(userId);
    }

    /** DELETE /api/auth/me → 204; deletes MY account (needs token + password again) */
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMe(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody DeleteAccountRequest request) {
        accountService.deleteOwnAccount(UUID.fromString(jwt.getSubject()), request.password());
    }
}
