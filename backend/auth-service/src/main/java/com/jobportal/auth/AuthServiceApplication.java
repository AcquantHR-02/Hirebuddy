package com.jobportal.auth;

import com.jobportal.shared.error.GlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.Clock;

/**
 * The start button of auth-service.
 * Running main() starts Spring and the web server inside it.
 */
@SpringBootApplication
// Switch on the shared error handler from shared-lib (it lives in another
// package, so component scan would not find it on its own).
@Import(GlobalExceptionHandler.class)
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }

    // The one clock the whole service uses to know "now" (always UTC).
    // Spring calls this once at startup and keeps the result as a bean.
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
