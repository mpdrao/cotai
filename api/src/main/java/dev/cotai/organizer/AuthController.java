package dev.cotai.organizer;

import dev.cotai.common.BusinessException;
import dev.cotai.common.ErrorCode;
import dev.cotai.organizer.AuthDtos.LoginRequest;
import dev.cotai.organizer.AuthDtos.OrganizerResponse;
import dev.cotai.organizer.AuthDtos.RegisterRequest;
import dev.cotai.organizer.AuthDtos.TokenResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;

/** UC01 Sign up / Log in. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final OrganizerRepository organizers;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Clock clock;

    public AuthController(OrganizerRepository organizers, PasswordEncoder passwordEncoder,
                          JwtService jwtService, Clock clock) {
        this.organizers = organizers;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.clock = clock;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public OrganizerResponse register(@Valid @RequestBody RegisterRequest body) {
        String email = Organizer.normalizeEmail(body.email());
        if (organizers.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED, "E-mail is already registered.");
        }
        Organizer organizer = organizers.save(new Organizer(
                body.name().trim(), email, passwordEncoder.encode(body.password()), clock.instant()));
        return OrganizerResponse.of(organizer);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest body) {
        // Same error for unknown e-mail and wrong password (no account enumeration).
        Organizer organizer = organizers.findByEmail(Organizer.normalizeEmail(body.email()))
                .filter(o -> passwordEncoder.matches(body.password(), o.getPasswordHash()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Invalid e-mail or password."));
        return new TokenResponse(jwtService.issue(organizer), "Bearer", jwtService.ttlSeconds());
    }
}
