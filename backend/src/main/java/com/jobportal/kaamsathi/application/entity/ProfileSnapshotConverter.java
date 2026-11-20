package com.jobportal.kaamsathi.application.entity;

import com.jobportal.kaamsathi.seeker.dto.ProfileSnapshot;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.databind.json.JsonMapper;

/**
 * ProfileSnapshot (Java record) ↔ JSON text in the `profile_snapshot` JSON column.
 * Uses Jackson 3 (Boot 4's JSON library). Records and LocalDate work out of the box
 * (dates are written as "2024-05-01", not numbers).
 */
@Converter
public class ProfileSnapshotConverter implements AttributeConverter<ProfileSnapshot, String> {

    // thread-safe once built, so one shared instance is fine
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Override
    public String convertToDatabaseColumn(ProfileSnapshot snapshot) {
        return snapshot == null ? null : JSON.writeValueAsString(snapshot);
    }

    @Override
    public ProfileSnapshot convertToEntityAttribute(String json) {
        return json == null ? null : JSON.readValue(json, ProfileSnapshot.class);
    }
}
