package com.jobportal.hirebuddy.auth.service;

import com.jobportal.hirebuddy.auth.dto.UserResponse;
import com.jobportal.hirebuddy.auth.entity.User;
import com.jobportal.hirebuddy.auth.entity.UserStatus;
import com.jobportal.hirebuddy.auth.repository.UserRepository;
import com.jobportal.hirebuddy.common.exception.ApiException;
import com.jobportal.hirebuddy.common.exception.ErrorCode;
import com.jobportal.hirebuddy.common.security.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * User data for everyone. Other modules call THIS (never UserRepository) when they need
 * user info or must change the user's company link.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public UserService(UserRepository userRepository, TokenService tokenService,
                       PasswordEncoder passwordEncoder, Clock clock) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /**
     * Ownership transfer (roles live in auth, so auth does this part):
     * owner's password checked again (401); new owner must be an ACTIVE RECRUITER of this company (404 / 409);
     * owner → RECRUITER (stays in the company), recruiter → COMPANY_OWNER; both logged out (roles changed).
     */
    @Transactional
    public void transferOwnership(UUID companyId, UUID ownerId, String password, UUID newOwnerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_TOKEN));
        if (!passwordEncoder.matches(password, owner.getPasswordHash())) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }
        User newOwner = userRepository.findById(newOwnerId)
                .filter(u -> u.getRole() == Role.RECRUITER && companyId.equals(u.getCompanyId()))
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        if (newOwner.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, "The new owner's account must be active");
        }

        Instant now = clock.instant();
        owner.changeRole(Role.RECRUITER, now);
        newOwner.changeRole(Role.COMPANY_OWNER, now);
        tokenService.revokeAll(owner.getId());
        tokenService.revokeAll(newOwner.getId());
    }

    // team list: all RECRUITERs of this company, oldest first
    @Transactional(readOnly = true)
    public List<UserResponse> listRecruiters(UUID companyId) {
        return userRepository.findByCompanyIdAndRoleOrderByCreatedAtAsc(companyId, Role.RECRUITER).stream()
                .map(UserResponse::from)
                .toList();
    }

    // Owner removes a recruiter: detach from the company + log out everywhere. Account stays.
    // Not a RECRUITER of THIS company → 404 (don't tell outsiders who exists).
    @Transactional
    public void removeRecruiter(UUID companyId, UUID recruiterId) {
        User recruiter = userRepository.findById(recruiterId)
                .filter(u -> u.getRole() == Role.RECRUITER && companyId.equals(u.getCompanyId()))
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        recruiter.leaveCompany(clock.instant());
        tokenService.revokeAll(recruiter.getId());   // refresh stops; access token dies within 15 min
    }

    // readOnly: only SELECT here, so Hibernate skips dirty checking (a bit faster)
    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        return userRepository.findById(id)
                .map(UserResponse::from)
                // token is valid but the account is gone (e.g. deleted) → treat the token as invalid
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_TOKEN));
    }

    // Called by the company module when an owner registers a company.
    // Joins the caller's transaction: if the company save fails, this is rolled back too.
    @Transactional
    public void assignCompany(UUID ownerId, UUID companyId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        if (owner.getRole() != Role.COMPANY_OWNER || owner.getCompanyId() != null) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED);   // not an owner, or already has a company
        }
        owner.assignCompany(companyId, clock.instant());   // no save(): dirty checking writes the UPDATE at commit
    }
}
