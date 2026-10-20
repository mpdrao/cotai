package dev.cotai.mockgateway;

import dev.cotai.common.Crypto;
import dev.cotai.payment.PaymentProperties;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Calls our own {@code /api/webhooks/payments} over real HTTP, asynchronously and signed with
 * HMAC-SHA256 over the raw body, so the call goes through the security filter and signature check.
 */
@Profile("dev")
@Component
public class WebhookDispatcher {

    private static final Logger log = LoggerFactory.getLogger(WebhookDispatcher.class);

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final PaymentProperties paymentProperties;
    private final JsonMapper jsonMapper;
    private final Clock clock;
    private final RestClient client;
    private final long delayMs;
    private final int duplicateDeliveries;

    public WebhookDispatcher(PaymentProperties paymentProperties, JsonMapper jsonMapper, Clock clock,
                             @Value("${server.port:8080}") int serverPort,
                             @Value("${mock-gateway.webhook-delay-ms:1500}") long delayMs,
                             @Value("${mock-gateway.duplicate-deliveries:1}") int duplicateDeliveries) {
        this.paymentProperties = paymentProperties;
        this.jsonMapper = jsonMapper;
        this.clock = clock;
        this.client = RestClient.builder().baseUrl("http://localhost:" + serverPort).build();
        this.delayMs = delayMs;
        this.duplicateDeliveries = Math.max(duplicateDeliveries, 1);
    }

    public void dispatch(String type, String providerReference, long amountCents) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId", "evt_" + UUID.randomUUID().toString().replace("-", ""));
        payload.put("type", type);
        payload.put("providerReference", providerReference);
        payload.put("amountCents", amountCents);
        payload.put("occurredAt", clock.instant().toString());

        byte[] body = jsonMapper.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8);
        String signature = "sha256=" + Crypto.hmacSha256Hex(paymentProperties.webhookSecret(), body);

        // Same eventId N times: demonstrates idempotency live (EC08).
        for (int i = 0; i < duplicateDeliveries; i++) {
            scheduler.schedule(() -> send(body, signature), delayMs + i * 200L, TimeUnit.MILLISECONDS);
        }
    }

    /** Public so tests can send arbitrary signed bodies (e.g. wrong amount). */
    public void send(byte[] body, String signature) {
        try {
            client.post().uri("/api/webhooks/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Signature", signature)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException e) {
            log.warn("Mock webhook delivery failed: {}", e.getMessage());
        }
    }

    @PreDestroy
    void shutdown() {
        scheduler.shutdownNow();
    }
}
