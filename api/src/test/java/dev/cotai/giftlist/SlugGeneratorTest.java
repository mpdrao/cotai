package dev.cotai.giftlist;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SlugGeneratorTest {

    private final SlugGenerator generator = new SlugGenerator();

    @Test
    void slugMatchesPublicContract() {
        String slug = generator.generate("Churrasco do Érick!");
        assertThat(slug).matches("^[a-z0-9-]+-[a-z2-7]{4}$").startsWith("churrasco-do-erick-");
    }

    @Test
    void emptyTitleFallsBackToList() {
        assertThat(SlugGenerator.prefix("!!!")).isEqualTo("lista");
    }
}
