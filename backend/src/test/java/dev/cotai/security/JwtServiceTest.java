package dev.cotai.security;

import dev.cotai.common.AppProperties;
import dev.cotai.organizer.Organizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidationException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private final AppProperties props = new AppProperties(
            ZoneOffset.UTC,
            "http://localhost:8080",
            new AppProperties.Jwt("test-secret-at-least-32-chars-long!", 3600),
            "test-manage-secret"
    );
    private final SecurityConfig config = new SecurityConfig();
    private final JwtEncoder encoder = config.jwtEncoder(props);
    private final JwtDecoder decoder = config.jwtDecoder(props);
    private final Organizer ana = new Organizer("Ana", "ana@mail.com", "hash", Instant.now());

    @Test
    @DisplayName("issued token carries the organizer id, name and our issuer")
    void issuedTokenCanBeRead() {
        JwtService jwtService = new JwtService(encoder, props, Clock.systemUTC());

        Jwt jwt = decoder.decode(jwtService.issue(ana));

        assertThat(JwtService.organizerId(jwt)).isEqualTo(ana.getId());
        assertThat(jwt.getClaimAsString("name")).isEqualTo("Ana");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("cotai");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofHours(1));
    }

    @Test
    @DisplayName("expired token is refused")
    void expiredTokenIsRefused() {
        Clock lastWeek = Clock.fixed(Instant.now().minus(Duration.ofDays(7)), ZoneOffset.UTC);
        String token = new JwtService(encoder, props, lastWeek).issue(ana);

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtValidationException.class);
    }

    @Test
    @DisplayName("token signed with our key but issued by someone else is refused")
    void tokenFromAnotherIssuerIsRefused() {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("someone-else")
                .subject(ana.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(600))
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtValidationException.class);
    }
}
