package com.jobportal.hirebuddy.auth.service;

import java.util.UUID;

/**
 * What auth needs to know about a company, without depending on the company module.
 * Auth OWNS this interface; the company module IMPLEMENTS it (CompanyService).
 * So the arrow stays one way: company → auth (dependency inversion).
 */
public interface CompanyVerificationCheck {

    /** true only if the company exists and an admin has VERIFIED it */
    boolean isVerified(UUID companyId);
}
