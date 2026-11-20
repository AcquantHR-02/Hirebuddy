package com.jobportal.kaamsathi.seeker.service;

import com.jobportal.kaamsathi.common.dto.NamedRef;
import com.jobportal.kaamsathi.common.exception.ApiException;
import com.jobportal.kaamsathi.common.exception.ErrorCode;
import com.jobportal.kaamsathi.file.dto.FileContent;
import com.jobportal.kaamsathi.file.dto.FileResponse;
import com.jobportal.kaamsathi.file.entity.FileKind;
import com.jobportal.kaamsathi.file.service.FileService;
import com.jobportal.kaamsathi.masterdata.service.CityService;
import com.jobportal.kaamsathi.masterdata.service.SkillService;
import com.jobportal.kaamsathi.seeker.dto.ApplicantData;
import com.jobportal.kaamsathi.seeker.dto.EducationItem;
import com.jobportal.kaamsathi.seeker.dto.ExperienceItem;
import com.jobportal.kaamsathi.seeker.dto.ProfileSnapshot;
import com.jobportal.kaamsathi.seeker.dto.SeekerProfileRequest;
import com.jobportal.kaamsathi.seeker.dto.SeekerProfileResponse;
import com.jobportal.kaamsathi.seeker.entity.Education;
import com.jobportal.kaamsathi.seeker.entity.Experience;
import com.jobportal.kaamsathi.seeker.entity.SeekerProfile;
import com.jobportal.kaamsathi.seeker.repository.SeekerProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Seeker profile rules: read my profile, save my profile (create on first save).
 */
@Service
public class SeekerProfileService {

    private final SeekerProfileRepository profileRepository;
    private final CityService cityService;            // masterdata: SERVICES only
    private final SkillService skillService;
    private final FileService fileService;            // file module: SERVICE only
    private final Clock clock;

    public SeekerProfileService(SeekerProfileRepository profileRepository, CityService cityService,
                                SkillService skillService, FileService fileService, Clock clock) {
        this.profileRepository = profileRepository;
        this.cityService = cityService;
        this.skillService = skillService;
        this.fileService = fileService;
        this.clock = clock;
    }

    /** My profile. Never saved yet → an empty one (200, complete = false), not 404: the screen just shows a blank form. */
    @Transactional(readOnly = true)
    public SeekerProfileResponse getMine(UUID userId) {
        return profileRepository.findById(userId)
                .map(this::toResponse)
                .orElseGet(() -> empty(userId));
    }

    /** Save the whole profile (first save creates it). Unknown / hidden city or skill → 400. */
    @Transactional
    public SeekerProfileResponse saveMine(UUID userId, SeekerProfileRequest r) {
        checkMasterdata(r);

        Instant now = clock.instant();
        SeekerProfile profile = profileRepository.findById(userId)
                .orElseGet(() -> new SeekerProfile(userId, now));

        profile.update(
                r.fullName().trim(),
                r.phone(),
                clean(r.headline()),
                clean(r.about()),
                r.cityId(),
                r.experienceYears().shortValue(),
                r.expectedSalary(),
                r.skillIds(),
                r.preferredCityIds(),
                r.education().stream().map(SeekerProfileService::toEntity).toList(),
                r.experience().stream().map(SeekerProfileService::toEntity).toList(),
                now);
        profileRepository.saveAndFlush(profile);      // INSERT the first time, UPDATE later
        return toResponse(profile);
    }

    /**
     * Upload / replace my resume (pdf / docx, ≤ 5 MB). Profile must be saved first (409): the row
     * needs a full name before it can exist. Old resume file is KEPT — Part 4 applications point to it.
     */
    @Transactional
    public SeekerProfileResponse uploadResume(UUID userId, MultipartFile upload) {
        SeekerProfile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.PROFILE_REQUIRED));
        FileResponse saved = fileService.upload(userId, FileKind.RESUME, upload);
        profile.changeResume(saved.id(), clock.instant());
        return toResponse(profile);
    }

    /** Download my current resume. No profile or no resume yet → 404. */
    @Transactional(readOnly = true)
    public FileContent openResume(UUID userId) {
        UUID resumeId = profileRepository.findById(userId)
                .map(SeekerProfile::getResumeFileId)
                .orElse(null);
        if (resumeId == null) {
            throw new ApiException(ErrorCode.FILE_NOT_FOUND, "You have not uploaded a resume yet");
        }
        return fileService.open(resumeId);
    }

    /**
     * For the application module: current resume + frozen profile copy (names filled in).
     * No profile or no resume → 409 PROFILE_INCOMPLETE.
     */
    @Transactional(readOnly = true)
    public ApplicantData snapshotForApply(UUID userId) {
        SeekerProfile profile = profileRepository.findById(userId)
                .filter(p -> p.getResumeFileId() != null)
                .orElseThrow(() -> new ApiException(ErrorCode.PROFILE_INCOMPLETE));
        SeekerProfileResponse r = toResponse(profile);       // reuse: names already resolved here
        ProfileSnapshot snapshot = new ProfileSnapshot(
                r.fullName(), r.phone(), r.headline(), r.about(), r.city(),
                r.experienceYears(), r.expectedSalary(),
                r.skills(), r.preferredCities(), r.education(), r.experience());
        return new ApplicantData(profile.getResumeFileId(), snapshot);
    }

    // ---- helpers ----

    private void checkMasterdata(SeekerProfileRequest r) {
        if (r.cityId() != null && !cityService.isActive(r.cityId())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "cityId: unknown or inactive city");
        }
        if (!r.skillIds().isEmpty() && !skillService.areAllActive(r.skillIds())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "skillIds: unknown or inactive skill");
        }
        if (!cityService.areAllActive(r.preferredCityIds())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "preferredCityIds: unknown or inactive city");
        }
    }

    private SeekerProfileResponse toResponse(SeekerProfile p) {
        Set<UUID> cityIds = new HashSet<>(p.getPreferredCityIds());
        if (p.getCityId() != null) {
            cityIds.add(p.getCityId());
        }
        Map<UUID, String> cities = cityService.labelsByIds(cityIds);          // 1 query for all cities
        Map<UUID, String> skills = skillService.namesByIds(p.getSkillIds());  // 1 query for all skills

        return new SeekerProfileResponse(
                p.getUserId(),
                p.getFullName(),
                p.getPhone(),
                p.getHeadline(),
                p.getAbout(),
                p.getCityId() == null ? null : new NamedRef(p.getCityId(), cities.get(p.getCityId())),
                (int) p.getExperienceYears(),
                p.getExpectedSalary(),
                refs(p.getSkillIds(), skills),
                refs(p.getPreferredCityIds(), cities),
                p.getEducation().stream().map(SeekerProfileService::toItem).toList(),
                p.getExperience().stream().map(SeekerProfileService::toItem).toList(),
                p.getResumeFileId(),
                p.getResumeFileId() != null,          // name is always set once saved; resume is the missing piece
                p.getUpdatedAt());
    }

    private static SeekerProfileResponse empty(UUID userId) {
        return new SeekerProfileResponse(userId, null, null, null, null, null, null, null,
                List.of(), List.of(), List.of(), List.of(), null, false, null);
    }

    private static List<NamedRef> refs(Collection<UUID> ids, Map<UUID, String> names) {
        return ids.stream()
                .map(id -> new NamedRef(id, names.get(id)))
                .sorted(Comparator.comparing(NamedRef::name, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private static Education toEntity(EducationItem e) {
        return new Education(e.degree().trim(), e.institute().trim(), clean(e.fieldOfStudy()),
                e.startYear().shortValue(), e.endYear() == null ? null : e.endYear().shortValue());
    }

    private static EducationItem toItem(Education e) {
        return new EducationItem(e.degree(), e.institute(), e.fieldOfStudy(),
                e.startYear().intValue(), e.endYear() == null ? null : e.endYear().intValue());
    }

    private static Experience toEntity(ExperienceItem e) {
        return new Experience(e.jobTitle().trim(), e.companyName().trim(), e.startDate(), e.endDate(),
                clean(e.description()));
    }

    private static ExperienceItem toItem(Experience e) {
        return new ExperienceItem(e.jobTitle(), e.companyName(), e.startDate(), e.endDate(), e.description());
    }

    // optional text: trim; "" / "   " → null
    private static String clean(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
