package com.jobportal.kaamsathi.masterdata.service;

import com.jobportal.kaamsathi.common.exception.ApiException;
import com.jobportal.kaamsathi.common.exception.ErrorCode;
import com.jobportal.kaamsathi.masterdata.dto.SkillResponse;
import com.jobportal.kaamsathi.masterdata.entity.Skill;
import com.jobportal.kaamsathi.masterdata.repository.SkillRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Skill list rules. Everyone can search; only the admin adds / renames / hides (checked in the controller).
 */
@Service
public class SkillService {

    private final SkillRepository skillRepository;
    private final Clock clock;

    public SkillService(SkillRepository skillRepository, Clock clock) {
        this.skillRepository = skillRepository;
        this.clock = clock;
    }

    // public dropdown: ACTIVE skills containing q (null / blank q = all active), A→Z, max 50
    @Transactional(readOnly = true)
    public List<SkillResponse> search(String q) {
        String text = (q == null) ? "" : q.trim();
        return skillRepository.findTop50ByActiveTrueAndNameContainingOrderByNameAsc(text).stream()
                .map(SkillResponse::from)
                .toList();
    }

    // admin screen: all skills, hidden ones too
    @Transactional(readOnly = true)
    public List<SkillResponse> listAll() {
        return skillRepository.findAllByOrderByNameAsc().stream()
                .map(SkillResponse::from)
                .toList();
    }

    @Transactional
    public SkillResponse create(String rawName) {
        String name = rawName.trim();                       // " Java " → "Java"
        if (skillRepository.existsByName(name)) {           // friendly check (case-insensitive in MySQL)
            throw new ApiException(ErrorCode.MASTERDATA_NAME_EXISTS);
        }
        Skill skill = new Skill(UUID.randomUUID(), name, clock.instant());
        try {
            skillRepository.saveAndFlush(skill);            // INSERT now → a race fails inside try
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.MASTERDATA_NAME_EXISTS);
        }
        return SkillResponse.from(skill);
    }

    @Transactional
    public SkillResponse rename(UUID id, String rawName) {
        Skill skill = find(id);
        String name = rawName.trim();
        if (skillRepository.existsByNameAndIdNot(name, id)) {   // same name on ANOTHER skill?
            throw new ApiException(ErrorCode.MASTERDATA_NAME_EXISTS);
        }
        skill.rename(name, clock.instant());
        try {
            skillRepository.flush();                        // UPDATE now → a race fails inside try
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.MASTERDATA_NAME_EXISTS);
        }
        return SkillResponse.from(skill);
    }

    @Transactional
    public SkillResponse activate(UUID id) {
        Skill skill = find(id);
        skill.activate(clock.instant());                    // dirty checking writes the UPDATE
        return SkillResponse.from(skill);
    }

    @Transactional
    public SkillResponse deactivate(UUID id) {
        Skill skill = find(id);
        skill.deactivate(clock.instant());
        return SkillResponse.from(skill);
    }

    private Skill find(UUID id) {
        return skillRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.MASTERDATA_NOT_FOUND));
    }
}
