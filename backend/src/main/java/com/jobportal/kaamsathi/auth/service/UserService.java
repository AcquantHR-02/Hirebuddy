package com.jobportal.kaamsathi.auth.service;

import com.jobportal.kaamsathi.auth.dto.UserResponse;
import com.jobportal.kaamsathi.auth.entity.User;
import com.jobportal.kaamsathi.auth.entity.UserStatus;
import com.jobportal.kaamsathi.auth.repository.UserRepository;
import com.jobportal.kaamsathi.common.exception.ApiException;
import com.jobportal.kaamsathi.common.exception.ErrorCode;
import com.jobportal.kaamsathi.common.security.Role;
import com.jobportal.kaamsathi.file.dto.FileContent;
import com.jobportal.kaamsathi.file.dto.FileResponse;
import com.jobportal.kaamsathi.file.entity.FileKind;
import com.jobportal.kaamsathi.file.service.FileService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * User data for everyone. Other modules call THIS (never UserRepository) when they need
 * user info or must change the user's company link.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final FileService fileService;
    private final Clock clock;

    public UserService(UserRepository userRepository, TokenService tokenService,
                       PasswordEncoder passwordEncoder, FileService fileService, Clock clock) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.fileService = fileService;
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

    /**
     * Upload / replace MY profile picture (any role). One tx: new file saved → user points to it →
     * old photo file deleted (its bytes only after commit). Bad file → 400 / 413 / 415 from FileService.
     */
    @Transactional
    public UserResponse changePhoto(UUID userId, MultipartFile upload) {
        User user = findMe(userId);
        UUID oldPhotoId = user.getPhotoFileId();
        FileResponse saved = fileService.upload(userId, FileKind.PHOTO, upload);
        user.changePhoto(saved.id(), clock.instant());
        if (oldPhotoId != null) {
            fileService.delete(oldPhotoId);    // nobody needs an old profile picture
        }
        return UserResponse.from(user);
    }

    /** Remove MY profile picture. No photo → nothing to do (still OK). */
    @Transactional
    public void removePhoto(UUID userId) {
        User user = findMe(userId);
        UUID oldPhotoId = user.getPhotoFileId();
        if (oldPhotoId != null) {
            user.changePhoto(null, clock.instant());
            fileService.delete(oldPhotoId);
        }
    }

    /** Someone's profile picture (any logged-in user may see it). Unknown user or no photo → 404. */
    @Transactional(readOnly = true)
    public FileContent openPhoto(UUID userId) {
        UUID photoId = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND))
                .getPhotoFileId();
        if (photoId == null) {
            throw new ApiException(ErrorCode.FILE_NOT_FOUND, "This user has no photo");
        }
        return fileService.open(photoId);
    }

    // the caller's own account (id from the JWT); gone → the token is no longer valid
    private User findMe(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_TOKEN));
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

    // for other modules (job): the user's company id from the DB (null = no company).
    // Read from DB, not the JWT claim: the token can be up to 15 min old (e.g. recruiter removed).
    @Transactional(readOnly = true)
    public UUID getCompanyId(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_TOKEN))
                .getCompanyId();
    }

    // for other modules (application notes): id → email for MANY users in ONE query (no N+1). Unknown ids are left out.
    @Transactional(readOnly = true)
    public Map<UUID, String> emailsByIds(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, User::getEmail));
    }
}
