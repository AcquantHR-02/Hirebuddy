package com.jobportal.kaamsathi.auth.controller;

import com.jobportal.kaamsathi.auth.dto.AcceptInviteRequest;
import com.jobportal.kaamsathi.auth.dto.InviteRequest;
import com.jobportal.kaamsathi.auth.dto.InviteResponse;
import com.jobportal.kaamsathi.auth.dto.UserResponse;
import com.jobportal.kaamsathi.auth.service.InviteService;
import com.jobportal.kaamsathi.auth.service.InviteService.InviteResult;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Recruiter invites: owner sends, recruiter accepts. HTTP only; rules live in InviteService.
 */
@RestController
@RequestMapping("/api/auth/invites")
public class InviteController {

    private static final String DEV_INVITE_TOKEN_HEADER = "X-Dev-Invite-Token";

    private final InviteService inviteService;
    private final boolean exposeTokens;

    public InviteController(InviteService inviteService,
                            @Value("${app.dev.expose-tokens}") boolean exposeTokens) {
        this.inviteService = inviteService;
        this.exposeTokens = exposeTokens;
    }

    /** POST /api/auth/invites → 201 + invite (COMPANY_OWNER only; others get 403) */
    @PostMapping
    @PreAuthorize("hasRole('COMPANY_OWNER')")
    public ResponseEntity<InviteResponse> invite(@AuthenticationPrincipal Jwt jwt,
                                                 @Valid @RequestBody InviteRequest request) {
        InviteResult result = inviteService.invite(UUID.fromString(jwt.getSubject()), request.email());
        ResponseEntity.BodyBuilder reply = ResponseEntity.status(HttpStatus.CREATED);
        if (exposeTokens) {
            reply.header(DEV_INVITE_TOKEN_HEADER, result.token());
        }
        return reply.body(result.invite());
    }

    /** POST /api/auth/invites/accept → 201 + the new RECRUITER user (public: the token is the proof) */
    @PostMapping("/accept")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse accept(@Valid @RequestBody AcceptInviteRequest request) {
        return inviteService.accept(request.token(), request.password());
    }
}
