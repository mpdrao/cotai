package dev.cotai.organizer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "organizer")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Organizer {

    @Id
    private UUID id;

    @Column(nullable = false, length = 80)
    private String name;

    /** Always lower-cased; unique index on lower(email). */
    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Organizer(String name, String email, String passwordHash, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.email = normalizeEmail(email);
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

}
