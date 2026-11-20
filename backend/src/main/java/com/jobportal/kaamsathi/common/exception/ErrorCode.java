package com.jobportal.kaamsathi.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Every error the app can return: a stable code (for the frontend) + its HTTP status + default message.
 */
public enum ErrorCode {

    NOT_FOUND(HttpStatus.NOT_FOUND, "No endpoint found for this URL"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method not allowed for this URL"),
    MALFORMED_JSON(HttpStatus.BAD_REQUEST, "Request body is missing or is not valid JSON"),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Some fields are not valid"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "An account with this email already exists"),
    // same message for wrong email AND wrong password, so nobody can find out which emails exist
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Email or password is wrong"),
    ACCOUNT_LOCKED(HttpStatus.TOO_MANY_REQUESTS, "Too many failed logins. Try again later"),
    ACCOUNT_SUSPENDED(HttpStatus.FORBIDDEN, "This account is suspended"),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "Please verify your email first"),
    // one 401 for missing, broken AND expired tokens: frontend refreshes on any 401
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Login required or the token is invalid/expired"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "You are not allowed to do this"),
    // emailed one-time link: unknown, already used or expired (one message for all three)
    INVALID_LINK(HttpStatus.BAD_REQUEST, "This link is invalid or has expired"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    // e.g. suspending or deleting an ADMIN account
    OPERATION_NOT_ALLOWED(HttpStatus.CONFLICT, "This action is not allowed for this account"),
    // owner must register a company (Phase 2) before inviting recruiters
    COMPANY_REQUIRED(HttpStatus.CONFLICT, "Register your company first"),
    COMPANY_ALREADY_EXISTS(HttpStatus.CONFLICT, "You have already registered a company"),
    GSTIN_ALREADY_EXISTS(HttpStatus.CONFLICT, "A company with this GSTIN is already registered"),
    COMPANY_NOT_FOUND(HttpStatus.NOT_FOUND, "Company not found"),
    // admin tries to verify / reject a company that was already reviewed
    COMPANY_NOT_PENDING(HttpStatus.CONFLICT, "This company is not waiting for review"),
    // owner edits a VERIFIED company (would need a new admin review; not allowed for now)
    COMPANY_ALREADY_VERIFIED(HttpStatus.CONFLICT, "A verified company can't be edited"),
    // e.g. inviting recruiters before the admin verified the company
    COMPANY_NOT_VERIFIED(HttpStatus.CONFLICT, "Your company must be verified by the admin first"),
    // masterdata (skills, industries, job categories, cities): one pair of codes for all lists
    MASTERDATA_NOT_FOUND(HttpStatus.NOT_FOUND, "Item not found"),
    MASTERDATA_NAME_EXISTS(HttpStatus.CONFLICT, "An item with this name already exists"),
    // free tier: 5 job posts per company, lifetime (Phase 4 plans raise it)
    FREE_JOB_LIMIT_REACHED(HttpStatus.CONFLICT, "Your company has used all 5 free job posts. Choose a plan to post more"),
    // job of another company or unknown id: same 404 (don't tell outsiders which jobs exist)
    JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "Job not found"),
    // e.g. closing a job that is not ACTIVE, reopening one that is not CLOSED or already past its expiry
    JOB_INVALID_STATE(HttpStatus.CONFLICT, "This action is not allowed in the job's current status"),
    // email OTP (register page): wrong, used up (5 wrong tries) or expired — one message for all
    INVALID_OTP(HttpStatus.BAD_REQUEST, "The code is wrong or has expired. Ask for a new code"),
    // a new OTP was asked for less than 60 seconds after the last one
    OTP_RESEND_TOO_SOON(HttpStatus.TOO_MANY_REQUESTS, "Please wait a minute before asking for a new code"),
    // only the first admin (ADMIN_EMAIL in .env) may add admins
    SUPER_ADMIN_ONLY(HttpStatus.FORBIDDEN, "Only the main admin can do this"),
    // uploads (file module)
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "File not found"),
    PROFILE_REQUIRED(HttpStatus.CONFLICT, "Save your profile first"),
    PROFILE_INCOMPLETE(HttpStatus.CONFLICT, "Complete your profile and upload a resume before applying"),
    ALREADY_APPLIED(HttpStatus.CONFLICT, "You have already applied to this job"),
    APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Application not found"),
    APPLICATION_INVALID_STATE(HttpStatus.CONFLICT, "This action is not allowed in the application's current status"),
    NOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "Note not found"),
    // payment: plans (admin)
    PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "Plan not found"),
    PLAN_NAME_EXISTS(HttpStatus.CONFLICT, "A plan with this name already exists"),
    // payment: requests (owner). Max 1 pending per company (DB UNIQUE on pending_company_id)
    PAYMENT_ALREADY_PENDING(HttpStatus.CONFLICT, "You already have a payment waiting for admin approval"),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "File is too large"),
    FILE_TYPE_NOT_ALLOWED(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "This file type is not allowed"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
