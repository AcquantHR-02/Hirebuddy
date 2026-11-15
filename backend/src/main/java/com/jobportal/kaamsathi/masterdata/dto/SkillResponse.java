package com.jobportal.kaamsathi.masterdata.dto;

import com.jobportal.kaamsathi.masterdata.entity.Skill;

import java.util.UUID;

/**
 * What we send back about a skill. Timestamps are left out: dropdowns don't need them.
 */
public record SkillResponse(
        UUID id,
        String name,
        boolean active
) {

    // entity → DTO (called inside the service, so the entity never leaves it)
    public static SkillResponse from(Skill skill) {
        return new SkillResponse(skill.getId(), skill.getName(), skill.isActive());
    }
}
