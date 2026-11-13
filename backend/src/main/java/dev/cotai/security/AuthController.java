package dev.cotai.security;

import dev.cotai.common.BusinessException;
import dev.cotai.common.ErrorCode;
import dev.cotai.organizer.Organizer;
import dev.cotai.organizer.OrganizerRepository;
import dev.cotai.security.AuthDtos.LoginRequest;
import dev.cotai.security.AuthDtos.OrganizerResponse;
import dev.cotai.security.AuthDtos.RegisterRequest;
import dev.cotai.security.AuthDtos.TokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final OrganizerRepository organizers;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Clock clock;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizerResponse register(@Valid @RequestBody RegisterRequest body) {
        String email = Organizer.normalizeEmail(body.email());
        if (organizers.existsByEmail(email)) {
            throw emailAlreadyRegistered();
        }

        Organizer organizer = new Organizer(body.name(), email, passwordEncoder.encode(body.password()), clock.instant());
        try {
            organizer = organizers.saveAndFlush(organizer);
        } catch (DataIntegrityViolationException e) {
            // Two sign ups with the same e-mail at the same time: the unique index lets only one through.
            throw emailAlreadyRegistered();
        }
        return OrganizerResponse.of(organizer);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest body) {
        // Same answer for unknown e-mail and wrong password, so nobody can probe which accounts exist.
        Organizer organizer = organizers.findByEmail(Organizer.normalizeEmail(body.email()))
                .filter(o -> passwordEncoder.matches(body.password(), o.getPasswordHash()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Invalid e-mail or password."));
        return new TokenResponse(jwtService.issue(organizer), "Bearer", jwtService.ttlSeconds());
    }

    private static BusinessException emailAlreadyRegistered() {
        return new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED, "E-mail is already registered.");
    }
}
