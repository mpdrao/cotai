package dev.cotai.common;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Serves the canonical OpenAPI 3.1 contract ({@code docs/api/openapi.yaml}) and an interactive
 * Swagger UI page at {@code /swagger-ui}.
 */
@Controller
public class OpenApiDocController {

    private static final MediaType YAML_MEDIA_TYPE = MediaType.parseMediaType("application/yaml;charset=UTF-8");

    @GetMapping({"/swagger-ui", "/swagger-ui/", "/swagger-ui.html"})
    public String swaggerUi() {
        return "swagger-ui";
    }

    @GetMapping({"/openapi.yaml", "/v3/api-docs.yaml"})
    @ResponseBody
    public ResponseEntity<Resource> openApiYaml() {
        Resource resource = resolveOpenApiResource();
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(YAML_MEDIA_TYPE)
                .cacheControl(CacheControl.noCache())
                .body(resource);
    }

    private Resource resolveOpenApiResource() {
        for (String candidate : new String[]{"../docs/api/openapi.yaml", "docs/api/openapi.yaml"}) {
            Path path = Path.of(candidate);
            if (Files.isRegularFile(path)) {
                return new FileSystemResource(path);
            }
        }
        return new ClassPathResource("openapi.yaml");
    }
}
