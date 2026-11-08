package com.jobportal.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The start button of auth-service.
 * Running main() starts Spring and the web server inside it.
 * Beans (Clock, PasswordEncoder, error handler) live in config/AppConfig.
 */
@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
