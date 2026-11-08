package com.jobportal.auth.token;

import com.jobportal.auth.token.dto.LoginRequest;
import com.jobportal.auth.token.dto.RefreshRequest;
import com.jobportal.auth.token.dto.TokenResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP doors for login, refresh and logout (same for all roles). Rules live in the services.
 */
@RestController
@RequestMapping("/auth")
public class TokenController {

    private final LoginService loginService;
    private final TokenService tokenService;

    public TokenController(LoginService loginService, TokenService tokenService) {
        this.loginService = loginService;
        this.tokenService = tokenService;
    }

    // POST /auth/login → 200 {accessToken, refreshToken, expiresIn}
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return loginService.login(request);
    }

    // POST /auth/refresh → 200 new pair; the sent refresh token stops working
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return tokenService.refresh(request.refreshToken());
    }

    // POST /auth/logout → 204 No Content (always, even for an unknown token)
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest request) {
        tokenService.logout(request.refreshToken());
    }
}
