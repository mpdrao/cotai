package dev.cotai.security;

import dev.cotai.common.BusinessException;
import dev.cotai.common.ErrorCode;
import dev.cotai.organizer.Organizer;
import dev.cotai.organizer.OrganizerRepository;
import dev.cotai.security.dto.LoginRequest;
import dev.cotai.security.dto.OrganizerResponse;
import dev.cotai.security.dto.RegisterRequest;
import dev.cotai.security.dto.TokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final OrganizerRepository organizers;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Clock clock;

    @Transactional
    public OrganizerResponse register(RegisterRequest body) {
        String email = Organizer.normalizeEmail(body.email());
        if (organizers.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED, "E-mail is already registered.");
        }
        Organizer organizer = organizers.save(new Organizer(
                body.name().trim(), email, passwordEncoder.encode(body.password()), clock.instant()));
        return OrganizerResponse.of(organizer);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest body) {

        Organizer organizer = organizers.findByEmail(Organizer.normalizeEmail(body.email()))
                .filter(o -> passwordEncoder.matches(body.password(), o.getPasswordHash()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Invalid e-mail or password."));
        return new TokenResponse(jwtService.issue(organizer), "Bearer", jwtService.ttlSeconds());
    }
}
