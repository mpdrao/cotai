
package dev.cotai.proxy;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@RestController
@RequestMapping("/api/proxy")
public class ProxyDiagnosticController {

    private final RestClient restClient;
    private final boolean proxyEnabled;
    private static final Logger log =
        LoggerFactory.getLogger(ProxyDiagnosticController.class);

    public ProxyDiagnosticController(
            @Qualifier("cotaiExternalRestClient") RestClient restClient,
            @Value("${cotai.proxy.enabled:false}") boolean proxyEnabled) {
        this.restClient = restClient;
        this.proxyEnabled = proxyEnabled;
    }

    @GetMapping("/test")
    public ResponseEntity<?> testProxy() {
        if (!proxyEnabled) {
            return ResponseEntity.badRequest().body(
                Map.of("success", false,
                       "message", "Proxy desativado no backend")
            );
        }

        try {
            restClient.get()
                    .uri("https://example.com")
                    .retrieve()
                    .toBodilessEntity();

            return ResponseEntity.ok(
                Map.of("success", true,
                       "message", "Conexão externa realizada com sucesso")
            );

        } catch (Exception e) {
    log.error("Falha no teste de conexão externa do COTAI", e);

    return ResponseEntity.status(502).body(
        Map.of(
            "success", false,
            "message", "Falha na conexão externa pelo proxy"
        )
    );
}
    }
}
