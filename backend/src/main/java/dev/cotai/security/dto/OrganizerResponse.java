package dev.cotai.security.dto;

import dev.cotai.organizer.Organizer;

import java.util.UUID;

public record OrganizerResponse(UUID id, String name, String email) {
    public static OrganizerResponse of(Organizer o) {
        return new OrganizerResponse(o.getId(), o.getName(), o.getEmail());
    }
}
