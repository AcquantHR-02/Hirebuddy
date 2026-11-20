package com.jobportal.kaamsathi.seeker.dto;

import java.util.UUID;

/**
 * What the application module freezes when a seeker applies: the current resume + a profile copy.
 */
public record ApplicantData(
        UUID resumeFileId,
        ProfileSnapshot snapshot
) {
}
