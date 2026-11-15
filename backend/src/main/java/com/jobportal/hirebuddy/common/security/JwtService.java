package com.jobportal.hirebuddy.common.security;

import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Makes access tokens (JWT, RS256). JwtEncoder does the signing with the private key.
 * Takes plain values (not the User entity), so common/ does not depend on the auth module.
 */
@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;
    private final Clock clock;

    public JwtService(JwtEncoder jwtEncoder, JwtProperties properties, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    /** companyId may be null (seekers, admins) */
    public String createAccessToken(UUID userId, Role role, UUID companyId) {
        Instant now = Instant.now(clock);

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .subject(userId.toString())                       // "sub": who
                .claim("role", role.name())                       // what they may do
                .issuedAt(now)                                    // "iat": made at
                .expiresAt(now.plus(properties.accessTokenTtl())); // "exp": dies after 15 min
        if (companyId != null) {                                  // only owners / recruiters
            claims.claim("companyId", companyId.toString());
        }

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }
}
