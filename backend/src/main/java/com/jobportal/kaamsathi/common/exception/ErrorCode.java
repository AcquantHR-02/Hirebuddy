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
