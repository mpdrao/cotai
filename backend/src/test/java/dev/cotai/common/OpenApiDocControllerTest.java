package dev.cotai.common;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiDocControllerTest {

    private final OpenApiDocController controller = new OpenApiDocController();

    @Test
    void returnsSwaggerUiTemplateName() {
        assertThat(controller.swaggerUi()).isEqualTo("swagger-ui");
    }

    @Test
    void servesOpenApiYamlContract() throws Exception {
        ResponseEntity<Resource> response = controller.openApiYaml();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        String yaml = response.getBody().getContentAsString(StandardCharsets.UTF_8);
        assertThat(yaml).contains("openapi: 3.1.0");
        assertThat(yaml).contains("/api/public/items/{itemId}/reservations");
    }
}
