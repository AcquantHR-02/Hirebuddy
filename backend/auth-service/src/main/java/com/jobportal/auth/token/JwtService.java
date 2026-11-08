package com.jobportal.auth.token;

import com.jobportal.auth.user.User;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

/**
 * Makes access tokens (JWT, RS256, 15 min). Spring's JwtEncoder does the signing.
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

    public String createAccessToken(User user) {
        Instant now = Instant.now(clock);

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .subject(user.getId().toString())                 // who
                .claim("role", user.getRole().name())             // what they may do
                .issuedAt(now)
                .expiresAt(now.plus(properties.accessTokenTtl()));
        if (user.getCompanyId() != null) {                        // only recruiters / owners
            claims.claim("companyId", user.getCompanyId().toString());
        }

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }
}
