package com.jobportal.kaamsathi.job.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * Composite primary key of saved_jobs: (seeker_id, job_id).
 * A record gives equals/hashCode for free — JPA needs them to compare keys.
 */
@Embeddable
public record SavedJobId(

        @JdbcTypeCode(SqlTypes.CHAR)
        @Column(name = "seeker_id", length = 36)
        UUID seekerId,

        @JdbcTypeCode(SqlTypes.CHAR)
        @Column(name = "job_id", length = 36)
        UUID jobId
) {
}
