package com.jobportal.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The start button of the gateway (port 8080): the only public door of the backend.
 * Routes are in application.yml; the JWT check is in security/JwtAuthFilter.
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
