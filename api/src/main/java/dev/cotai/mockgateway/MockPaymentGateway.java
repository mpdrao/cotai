package dev.cotai.mockgateway;

import dev.cotai.common.AppProperties;
import dev.cotai.common.Crypto;
import dev.cotai.payment.PaymentGateway;
import dev.cotai.payment.PaymentProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fake payment provider that lives inside the same JAR (dev profile only). It keeps charges in
 * memory and calls our own webhook over HTTP, exactly like a real provider would.
 */
@Profile("dev")
@Component
@RequiredArgsConstructor
public class MockPaymentGateway implements PaymentGateway {

    public enum ChargeStatus { PENDING, PAID, DECLINED }

    public static final class MockCharge {
        private final String providerReference;
        private final long amountCents;
        private final Instant expiresAt;
        private final String checkoutUrl;
        private final String description;
        private volatile ChargeStatus status = ChargeStatus.PENDING;

        MockCharge(String providerReference, long amountCents, Instant expiresAt, String checkoutUrl,
                   String description) {
            this.providerReference = providerReference;
            this.amountCents = amountCents;
            this.expiresAt = expiresAt;
            this.checkoutUrl = checkoutUrl;
            this.description = description;
        }

        public String providerReference() { return providerReference; }
        public long amountCents() { return amountCents; }
        public Instant expiresAt() { return expiresAt; }
        public String checkoutUrl() { return checkoutUrl; }
        public String description() { return description; }
        public ChargeStatus status() { return status; }
    }

    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";

    private final Map<String, MockCharge> charges = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final AppProperties appProperties;
    private final PaymentProperties paymentProperties;
    private final WebhookDispatcher dispatcher;
    private final Clock clock;

    @Override
    public String providerName() {
        return "mock";
    }

    @Override
    public Charge createCharge(ChargeRequest request) {
        String reference = "mock_" + randomChars(20); // matches ^mock_[a-z0-9]{20}$
        // The QR code points to app.public-base-url: use the LAN IP if a phone will scan it (EC27).
        String url = appProperties.publicBaseUrl().replaceAll("/+$", "") + "/mock-gateway/checkout/" + reference;
        charges.put(reference, new MockCharge(reference, request.amountCents(), request.expiresAt(), url,
                request.description()));
        return new Charge(reference, url, url);
    }

    @Override
    public boolean isSignatureValid(byte[] rawBody, String signatureHeader) {
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            return false;
        }
        String expected = "sha256=" + Crypto.hmacSha256Hex(paymentProperties.webhookSecret(), rawBody);
        return Crypto.constantTimeEquals(expected, signatureHeader);
    }

    public Optional<MockCharge> find(String providerReference) {
        return Optional.ofNullable(charges.get(providerReference));
    }

    public boolean isExpired(MockCharge charge) {
        return !clock.instant().isBefore(charge.expiresAt());
    }

    /** Marks the charge as paid and schedules the signed {@code payment.paid} webhook. */
    public void approve(MockCharge charge) {
        charge.status = ChargeStatus.PAID;
        dispatcher.dispatch("payment.paid", charge.providerReference(), charge.amountCents());
    }

    public void decline(MockCharge charge) {
        charge.status = ChargeStatus.DECLINED;
        dispatcher.dispatch("payment.declined", charge.providerReference(), charge.amountCents());
    }

    private String randomChars(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
