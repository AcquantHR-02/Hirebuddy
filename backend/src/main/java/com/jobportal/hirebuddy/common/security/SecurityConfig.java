package com.jobportal.hirebuddy.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Who may call which URL. Every request passes through this filter chain BEFORE any controller.
 */
@Configuration
@EnableMethodSecurity   // allows @PreAuthorize("hasRole('ADMIN')") on controller/service methods
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityErrorHandler errors) throws Exception {
        http
                // token API: no browser cookies → CSRF attack not possible, so turn the check off
                .csrf(AbstractHttpConfigurer::disable)
                // no server session: every request must bring its own token
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // no login page, no browser pop-up
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // open to everyone
                        .requestMatchers("/api/auth/signup/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        // the access token may already be expired here; the refresh token in the body is the proof
                        .requestMatchers(HttpMethod.POST, "/api/auth/refresh", "/api/auth/logout").permitAll()
                        // clicked from an email: the user may not be logged in; the one-time token is the proof
                        .requestMatchers(HttpMethod.POST, "/api/auth/verify-email").permitAll()
                        // the user forgot the password, so they cannot be logged in
                        .requestMatchers(HttpMethod.POST, "/api/auth/forgot-password", "/api/auth/reset-password").permitAll()
                        // recruiter has no account yet; the invite token is the proof
                        .requestMatchers(HttpMethod.POST, "/api/auth/invites/accept").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        // everything else: must be logged in (safe default for new endpoints)
                        .anyRequest().authenticated())
                // read "Authorization: Bearer <token>" and check it with the JwtDecoder bean
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(errors))        // bad/expired token → 401 ApiError
                // no token at all → 401, wrong role → 403 (both as ApiError JSON)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(errors)
                        .accessDeniedHandler(errors));
        return http.build();
    }
}
