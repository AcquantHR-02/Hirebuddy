package com.jobportal.kaamsathi.application.entity;

/**
 * Where an application stands. Seeker tracker: Applied → Viewed → Shortlisted → Interview → Decision.
 */
public enum ApplicationStatus {
    APPLIED,             // just sent
    VIEWED,              // a recruiter opened it (first time)
    SHORTLISTED,
    INTERVIEW,
    OFFERED,
    HIRED,               // final
    REJECTED,            // final
    WITHDRAWN,           // final, by the seeker
    CLOSED_NO_RESPONSE;  // final, automatic after 14 days without recruiter action

    // finished: nothing can change any more (no withdraw, no stage move)
    public boolean isFinal() {
        return this == HIRED || this == REJECTED || this == WITHDRAWN || this == CLOSED_NO_RESPONSE;
    }

    // statuses a recruiter may set by hand (APPLIED / VIEWED / WITHDRAWN / CLOSED_NO_RESPONSE are set by the system or seeker)
    public boolean isRecruiterTarget() {
        return this == SHORTLISTED || this == INTERVIEW || this == OFFERED || this == HIRED || this == REJECTED;
    }

    // later in the pipeline? Uses the declaration order above (APPLIED first … HIRED). Keep the order!
    public boolean isAfter(ApplicationStatus other) {
        return this.ordinal() > other.ordinal();
    }
}
