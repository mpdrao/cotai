package dev.cotai.organizer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Request/response records of the auth endpoints. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 2, max = 80) String name,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 72) String password) {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record OrganizerResponse(UUID id, String name, String email) {
        static OrganizerResponse of(Organizer o) {
            return new OrganizerResponse(o.getId(), o.getName(), o.getEmail());
        }
    }

    public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
    }
}
