package dev.cotai.security;

import dev.cotai.common.AppProperties;
import dev.cotai.organizer.Organizer;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/** Issues HS256 JWTs whose subject is the organizer id. */
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtEncoder encoder;
    private final AppProperties props;
    private final Clock clock;

    public String issue(Organizer organizer) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("cotai")
                .subject(organizer.getId().toString())
                .claim("name", organizer.getName())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(ttlSeconds()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long ttlSeconds() {
        return props.jwt().ttlSeconds();
    }

    public static UUID organizerId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
