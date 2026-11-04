package com.jobportal.auth.demo;

import com.jobportal.shared.error.ApiException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TEMPORARY – only for learning. Shows one good reply and one error reply.
 * Delete this when real auth endpoints exist.
 */
@RestController
@RequestMapping("/auth/demo")
public class DemoController {

    // GET /auth/demo/ok → 200 with plain text
    @GetMapping("/ok")
    public String ok() {
        return "auth-service is alive";
    }

    // GET /auth/demo/fail → throws ApiException → GlobalExceptionHandler → 404 JSON
    @GetMapping("/fail")
    public String fail() {
        throw new ApiException(404, "NOT_FOUND", "Demo: this thing does not exist");
    }
}
