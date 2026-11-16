package dev.cotai.organizer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class OrganizerTest {

    @Test
    @DisplayName("normalizeEmail trims spaces and lowercases")
    void normalizeEmail() {
        assertThat(Organizer.normalizeEmail("  Ana.Souza@Mail.COM ")).isEqualTo("ana.souza@mail.com");
    }

    @Test
    @DisplayName("a new organizer keeps the e-mail normalized and the name trimmed")
    void newOrganizer() {
        Instant now = Instant.parse("2026-11-13T12:00:00Z");

        Organizer organizer = new Organizer(" Ana Souza ", "ANA@mail.com", "hash", now);

        assertThat(organizer.getId()).isNotNull();
        assertThat(organizer.getName()).isEqualTo("Ana Souza");
        assertThat(organizer.getEmail()).isEqualTo("ana@mail.com");
        assertThat(organizer.getCreatedAt()).isEqualTo(now);
    }
}
