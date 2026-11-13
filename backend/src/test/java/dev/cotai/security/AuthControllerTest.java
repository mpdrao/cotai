package dev.cotai.security;

import dev.cotai.common.AppProperties;
import dev.cotai.common.BusinessException;
import dev.cotai.common.ErrorCode;
import dev.cotai.organizer.Organizer;
import dev.cotai.organizer.OrganizerRepository;
import dev.cotai.security.AuthDtos.LoginRequest;
import dev.cotai.security.AuthDtos.OrganizerResponse;
import dev.cotai.security.AuthDtos.RegisterRequest;
import dev.cotai.security.AuthDtos.TokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private OrganizerRepository organizers;

    // Low cost factor, the tests don't need a slow hash.
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final Clock clock = Clock.fixed(Instant.now(), ZoneOffset.UTC);
    private final AppProperties props = new AppProperties(
            ZoneOffset.UTC,
            "http://localhost:8080",
            new AppProperties.Jwt("test-secret-at-least-32-chars-long!", 3600),
            "test-manage-secret"
    );

    private AuthController controller;
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        SecurityConfig config = new SecurityConfig();
        JwtService jwtService = new JwtService(config.jwtEncoder(props), props, clock);
        jwtDecoder = config.jwtDecoder(props);
        controller = new AuthController(organizers, passwordEncoder, jwtService, clock);
    }

    @Test
    @DisplayName("register saves the organizer with normalized e-mail and hashed password")
    void registerSavesOrganizer() {
        when(organizers.existsByEmail("ana@mail.com")).thenReturn(false);
        when(organizers.saveAndFlush(any(Organizer.class))).thenAnswer(inv -> inv.getArgument(0));

        OrganizerResponse response = controller.register(new RegisterRequest("Ana", " Ana@Mail.com ", "segredo123"));

        ArgumentCaptor<Organizer> saved = ArgumentCaptor.forClass(Organizer.class);
        verify(organizers).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("ana@mail.com");
        assertThat(saved.getValue().getPasswordHash()).isNotEqualTo("segredo123");
        assertThat(passwordEncoder.matches("segredo123", saved.getValue().getPasswordHash())).isTrue();
        assertThat(response.email()).isEqualTo("ana@mail.com");
        assertThat(response.name()).isEqualTo("Ana");
    }

    @Test
    @DisplayName("register refuses an e-mail that is already registered")
    void registerRefusesDuplicatedEmail() {
        when(organizers.existsByEmail("ana@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> controller.register(new RegisterRequest("Ana", "ANA@mail.com", "segredo123")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).code())
                .isEqualTo(ErrorCode.EMAIL_ALREADY_REGISTERED);
        verify(organizers, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("register answers 409 when a concurrent sign up wins the unique index")
    void registerLosesTheRaceForTheSameEmail() {
        when(organizers.existsByEmail("ana@mail.com")).thenReturn(false);
        when(organizers.saveAndFlush(any(Organizer.class)))
                .thenThrow(new DataIntegrityViolationException("uq_organizer_email"));

        assertThatThrownBy(() -> controller.register(new RegisterRequest("Ana", "ana@mail.com", "segredo123")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).code())
                .isEqualTo(ErrorCode.EMAIL_ALREADY_REGISTERED);
    }

    @Test
    @DisplayName("login with the right password returns a bearer token for the organizer")
    void loginReturnsToken() {
        Organizer ana = new Organizer("Ana", "ana@mail.com", passwordEncoder.encode("segredo123"), clock.instant());
        when(organizers.findByEmail("ana@mail.com")).thenReturn(Optional.of(ana));

        TokenResponse token = controller.login(new LoginRequest("Ana@Mail.com ", "segredo123"));

        assertThat(token.tokenType()).isEqualTo("Bearer");
        assertThat(token.expiresIn()).isEqualTo(3600);
        Jwt jwt = jwtDecoder.decode(token.accessToken());
        assertThat(JwtService.organizerId(jwt)).isEqualTo(ana.getId());
        assertThat(jwt.getClaimAsString("name")).isEqualTo("Ana");
    }

    @Test
    @DisplayName("login with a wrong password is refused")
    void loginWithWrongPassword() {
        Organizer ana = new Organizer("Ana", "ana@mail.com", passwordEncoder.encode("segredo123"), clock.instant());
        when(organizers.findByEmail("ana@mail.com")).thenReturn(Optional.of(ana));

        assertThatThrownBy(() -> controller.login(new LoginRequest("ana@mail.com", "outra-senha")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).code())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("login with an unknown e-mail gets the same error as a wrong password")
    void loginWithUnknownEmail() {
        when(organizers.findByEmail("ninguem@mail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.login(new LoginRequest("ninguem@mail.com", "segredo123")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).code())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("request records trim e-mail and name but keep the password as typed")
    void requestsTrimEmailButNotPassword() {
        RegisterRequest register = new RegisterRequest("  Ana ", " ana@mail.com ", " senha com espaco ");
        LoginRequest login = new LoginRequest(" ana@mail.com", " senha com espaco ");

        assertThat(register.name()).isEqualTo("Ana");
        assertThat(register.email()).isEqualTo("ana@mail.com");
        assertThat(register.password()).isEqualTo(" senha com espaco ");
        assertThat(login.email()).isEqualTo("ana@mail.com");
        assertThat(login.password()).isEqualTo(" senha com espaco ");
    }
}
