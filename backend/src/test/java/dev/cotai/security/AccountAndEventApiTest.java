package dev.cotai.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import dev.cotai.common.AppProperties;
import dev.cotai.common.ClockConfig;
import dev.cotai.eventtype.Event;
import dev.cotai.eventtype.EventTypeController;
import dev.cotai.eventtype.EventTypeRepository;
import dev.cotai.giftlist.GiftListController;
import dev.cotai.giftlist.GiftListDtos.UpdateGiftListRequest;
import dev.cotai.giftlist.GiftListService;
import dev.cotai.giftlist.Item;
import dev.cotai.giftlist.PublicGiftListController;
import dev.cotai.organizer.Organizer;
import dev.cotai.organizer.OrganizerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US-05, US-06 and US-07 through the real HTTP layer: validation, error codes and
 * who can call what (Usuário Externo, Organizador, Convidado). Repositories and the
 * gift list service are mocked, so no database is needed.
 */
@WebMvcTest(controllers = {
        AuthController.class, EventTypeController.class, GiftListController.class, PublicGiftListController.class})
@Import({SecurityConfig.class, JwtService.class, ClockConfig.class})
@EnableConfigurationProperties(AppProperties.class)
class AccountAndEventApiTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private OrganizerRepository organizers;
    @MockitoBean
    private EventTypeRepository eventTypes;
    @MockitoBean
    private GiftListService giftListService;

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private String bearer(Organizer organizer) {
        return "Bearer " + jwtService.issue(organizer);
    }

    @Nested
    @DisplayName("US-05 Cadastrar-se")
    class SignUp {

        @Test
        @DisplayName("creates the account with name, e-mail and password")
        void createsAccount() throws Exception {
            when(organizers.existsByEmail("ana@mail.com")).thenReturn(false);
            when(organizers.saveAndFlush(any(Organizer.class))).thenAnswer(inv -> inv.getArgument(0));

            mvc.perform(json(post("/api/auth/register"),
                            "{\"name\":\"Ana\",\"email\":\"Ana@Mail.com \",\"password\":\"segredo123\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Ana"))
                    .andExpect(jsonPath("$.email").value("ana@mail.com"))
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.passwordHash").doesNotExist());
        }

        @Test
        @DisplayName("refuses an e-mail that is already registered")
        void refusesDuplicatedEmail() throws Exception {
            when(organizers.existsByEmail("ana@mail.com")).thenReturn(true);

            mvc.perform(json(post("/api/auth/register"),
                            "{\"name\":\"Ana\",\"email\":\"ana@mail.com\",\"password\":\"segredo123\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
        }

        @Test
        @DisplayName("refuses the same e-mail when two sign ups race")
        void refusesDuplicatedEmailInARace() throws Exception {
            when(organizers.existsByEmail("ana@mail.com")).thenReturn(false);
            when(organizers.saveAndFlush(any(Organizer.class)))
                    .thenThrow(new DataIntegrityViolationException("uq_organizer_email"));

            mvc.perform(json(post("/api/auth/register"),
                            "{\"name\":\"Ana\",\"email\":\"ana@mail.com\",\"password\":\"segredo123\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
        }

        @Test
        @DisplayName("requires name, a valid e-mail and a password with 8+ characters")
        void validatesFields() throws Exception {
            mvc.perform(json(post("/api/auth/register"), "{\"name\":\" A \",\"email\":\"ana@\",\"password\":\"1234567\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("name", "email", "password")));
        }
    }

    @Nested
    @DisplayName("US-06 Fazer Login")
    class Login {

        private Organizer ana() {
            Organizer ana = new Organizer("Ana", "ana@mail.com", passwordEncoder.encode("segredo123"), Instant.now());
            when(organizers.findByEmail("ana@mail.com")).thenReturn(Optional.of(ana));
            return ana;
        }

        @Test
        @DisplayName("with the right e-mail and password, the token opens the organizer area")
        void rightCredentialsGiveAccess() throws Exception {
            ana();
            when(giftListService.listMine(any())).thenReturn(List.of());

            String response = mvc.perform(json(post("/api/auth/login"),
                            "{\"email\":\" ANA@mail.com\",\"password\":\"segredo123\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.accessToken", not(emptyString())))
                    .andReturn().getResponse().getContentAsString();
            String token = response.replaceAll(".*\"accessToken\":\"([^\"]+)\".*", "$1");

            mvc.perform(get("/api/gift-lists").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("wrong password is refused")
        void wrongPassword() throws Exception {
            ana();

            mvc.perform(json(post("/api/auth/login"), "{\"email\":\"ana@mail.com\",\"password\":\"errada123\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }

        @Test
        @DisplayName("unknown e-mail gets the same answer as a wrong password")
        void unknownEmail() throws Exception {
            when(organizers.findByEmail("ninguem@mail.com")).thenReturn(Optional.empty());

            mvc.perform(json(post("/api/auth/login"), "{\"email\":\"ninguem@mail.com\",\"password\":\"segredo123\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }
    }

    @Nested
    @DisplayName("Actors: who can call what")
    class Access {

        @Test
        @DisplayName("Usuário Externo (no login) cannot use the organizer area")
        void externalUserIsBlocked() throws Exception {
            mvc.perform(get("/api/gift-lists")).andExpect(status().isUnauthorized());
            mvc.perform(json(post("/api/gift-lists"), "{}")).andExpect(status().isUnauthorized());
            mvc.perform(json(patch("/api/gift-lists/" + UUID.randomUUID()), "{}")).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("a token that we did not sign is refused")
        void foreignTokenIsRefused() throws Exception {
            byte[] otherKey = "another-secret-with-at-least-32-bytes!".getBytes(StandardCharsets.UTF_8);
            NimbusJwtEncoder otherEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(otherKey, "HmacSHA256")));
            Instant now = Instant.now();
            String forged = otherEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
                    JwtClaimsSet.builder().issuer("cotai").subject(UUID.randomUUID().toString())
                            .issuedAt(now).expiresAt(now.plusSeconds(600)).build())).getTokenValue();

            mvc.perform(get("/api/gift-lists").header("Authorization", "Bearer " + forged))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Convidado opens the public list without sign up or login")
        void guestNeedsNoLogin() throws Exception {
            mvc.perform(get("/api/public/gift-lists/churrasco-da-firma")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("sign up, login and the event catalog are open to everyone")
        void publicEndpoints() throws Exception {
            when(eventTypes.findAllByOrderByNameAsc()).thenReturn(List.of());

            mvc.perform(get("/api/event-types")).andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("US-07 Gerenciar Evento")
    class ManageEvent {

        private final Organizer ana = new Organizer("Ana", "ana@mail.com", "hash", Instant.now());

        @Test
        @DisplayName("the catalog has the event types with the suggested items of each one")
        void catalogWithSuggestions() throws Exception {
            Event birthday = new Event("BIRTHDAY", "Aniversário", 30);
            ReflectionTestUtils.setField(birthday, "items", new ArrayList<>(List.of(
                    new Item(birthday, "Bolo", "unidade", 15000, null),
                    new Item(birthday, "Docinhos", "cento", 9000, null))));
            when(eventTypes.findAllByOrderByNameAsc()).thenReturn(List.of(birthday));
            when(eventTypes.findById("BIRTHDAY")).thenReturn(Optional.of(birthday));

            mvc.perform(get("/api/event-types"))
                    .andExpect(jsonPath("$[0].code").value("BIRTHDAY"))
                    .andExpect(jsonPath("$[0].name").value("Aniversário"))
                    .andExpect(jsonPath("$[0].defaultReservationTtlMinutes").value(30));
            mvc.perform(get("/api/event-types/birthday/template-items"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].name", containsInAnyOrder("Bolo", "Docinhos")));
        }

        @Test
        @DisplayName("the organizer can edit title, date and type of the event")
        void editsTitleDateAndType() throws Exception {
            UUID listId = UUID.randomUUID();

            mvc.perform(json(patch("/api/gift-lists/" + listId),
                            "{\"title\":\" Niver da Ana \",\"eventDate\":\"2099-12-20\",\"eventTypeCode\":\"birthday\"}")
                            .header("Authorization", bearer(ana)))
                    .andExpect(status().isOk());

            ArgumentCaptor<UpdateGiftListRequest> sent = ArgumentCaptor.forClass(UpdateGiftListRequest.class);
            verify(giftListService).update(eq(ana.getId()), eq(listId), sent.capture());
            assertThat(sent.getValue().title()).isEqualTo("Niver da Ana");
            assertThat(sent.getValue().eventDate()).hasToString("2099-12-20");
            assertThat(sent.getValue().eventTypeCode()).isEqualTo("BIRTHDAY");
        }

        @Test
        @DisplayName("a blank title is refused when editing")
        void blankTitleIsRefused() throws Exception {
            mvc.perform(json(patch("/api/gift-lists/" + UUID.randomUUID()), "{\"title\":\"     \"}")
                            .header("Authorization", bearer(ana)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }
    }
}
