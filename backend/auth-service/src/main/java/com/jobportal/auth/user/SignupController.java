package com.jobportal.auth.user;

import com.jobportal.auth.user.dto.SeekerSignupRequest;
import com.jobportal.auth.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP door for signup. It only receives the request and returns the reply;
 * all the rules live in SignupService.
 */
@RestController
@RequestMapping("/auth/signup")
public class SignupController {

    private final SignupService signupService;

    public SignupController(SignupService signupService) {
        this.signupService = signupService;
    }

    // POST /auth/signup/seeker  → 201 Created + UserResponse JSON
    @PostMapping("/seeker")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse signupSeeker(@Valid @RequestBody SeekerSignupRequest request) {
        User user = signupService.signupSeeker(request);
        return UserResponse.from(user);
    }
}
