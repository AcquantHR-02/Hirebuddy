package com.jobportal.hirebuddy.common.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.io.IOException;
import java.io.InputStream;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * The JWT tools (RS256): encoder SIGNS with the private key, decoder CHECKS with the public key.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)   // turn the app.jwt settings into a JwtProperties bean
public class JwtConfig {

    @Bean
    public JwtEncoder jwtEncoder(JwtProperties properties) {
        RSAPublicKey publicKey = read(RsaKeyConverters.x509(), properties.publicKey());
        RSAPrivateKey privateKey = read(RsaKeyConverters.pkcs8(), properties.privateKey());
        return NimbusJwtEncoder.withKeyPair(publicKey, privateKey).build();
    }

    // Spring Security uses this bean to check every "Authorization: Bearer <token>" (signature + expiry)
    @Bean
    public JwtDecoder jwtDecoder(JwtProperties properties) {
        RSAPublicKey publicKey = read(RsaKeyConverters.x509(), properties.publicKey());
        return NimbusJwtDecoder.withPublicKey(publicKey).build();
    }

    /**
     * Token → Spring's logged-in user. Reads our "role" claim ("SEEKER") and makes the
     * authority "ROLE_SEEKER", so hasRole('SEEKER') / @PreAuthorize work.
     * Spring Security picks this bean up automatically for oauth2ResourceServer().jwt().
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("role");   // default would be "scope"
        roles.setAuthorityPrefix("ROLE_");       // default would be "SCOPE_"

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }

    // open a .pem file and turn its text into a Java key object
    private static <T> T read(Converter<InputStream, T> converter, Resource file) {
        try (InputStream in = file.getInputStream()) {
            return converter.convert(in);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read key file: " + file, e);
        }
    }
}
