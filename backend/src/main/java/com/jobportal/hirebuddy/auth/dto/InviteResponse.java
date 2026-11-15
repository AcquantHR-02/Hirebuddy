package com.jobportal.hirebuddy.auth.dto;

import com.jobportal.hirebuddy.auth.entity.InviteStatus;
import com.jobportal.hirebuddy.auth.entity.RecruiterInvite;

import java.time.Instant;
import java.util.UUID;

/**
 * What the owner gets back after inviting. Never contains the token or its hash.
 */
public record InviteResponse(
        UUID id,
        String email,
        UUID companyId,
        InviteStatus status,
        Instant expiresAt
) {

    public static InviteResponse from(RecruiterInvite invite) {
        return new InviteResponse(
                invite.getId(),
                invite.getEmail(),
                invite.getCompanyId(),
                invite.getStatus(),
                invite.getExpiresAt());
    }
}
