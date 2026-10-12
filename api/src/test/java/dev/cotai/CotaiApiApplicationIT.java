package dev.cotai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/** Smoke test: the context starts, Flyway migrates and Hibernate validates the schema on real PostgreSQL. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("dev")
class CotaiApiApplicationIT {

    @Test
    void contextLoads() {
    }
}
