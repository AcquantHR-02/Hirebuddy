package com.jobportal.auth.config;

import com.jobportal.auth.token.JwtProperties;
import com.jobportal.shared.security.SecurityErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;

/**
 * Who may call what, and the JWT tools.
 * Public: signup, login, refresh, logout. Everything else needs "Authorization: Bearer <access token>".
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityErrorHandler errors) {
        http
                .csrf(AbstractHttpConfigurer::disable)            // no cookies/sessions -> CSRF not needed
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST,
                                "/auth/signup/**", "/auth/login", "/auth/refresh", "/auth/logout").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()).authenticationEntryPoint(errors))
                .exceptionHandling(e -> e.authenticationEntryPoint(errors).accessDeniedHandler(errors));
        return http.build();
    }

    @Bean
    public SecurityErrorHandler securityErrorHandler(JsonMapper jsonMapper, Clock clock) {
        return new SecurityErrorHandler(jsonMapper, clock);
    }

    // Signs tokens with the private key (RS256)
    @Bean
    public JwtEncoder jwtEncoder(JwtProperties properties) {
        return NimbusJwtEncoder.withKeyPair(publicKey(properties),
                read(RsaKeyConverters.pkcs8(), properties.privateKey())).build();
    }

    // Checks tokens (signature + expiry) with the public key, e.g. for GET /auth/me
    @Bean
    public JwtDecoder jwtDecoder(JwtProperties properties) {
        return NimbusJwtDecoder.withPublicKey(publicKey(properties)).build();
    }

    private static RSAPublicKey publicKey(JwtProperties properties) {
        return read(RsaKeyConverters.x509(), properties.publicKey());
    }

    private static <T> T read(Converter<InputStream, T> converter, Resource file) {
        try (InputStream in = file.getInputStream()) {
            return converter.convert(in);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read key file: " + file, e);
        }
    }
}
