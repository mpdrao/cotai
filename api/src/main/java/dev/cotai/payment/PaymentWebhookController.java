package dev.cotai.payment;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Called by the payment provider. The body is read as {@code byte[]} on purpose: if Spring bound it
 * to a DTO first, re-serializing would change the bytes and the HMAC would no longer match.
 */
@RestController
@RequestMapping("/api/webhooks")
public class PaymentWebhookController {

    private final PaymentService paymentService;

    public PaymentWebhookController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/payments")
    public ResponseEntity<Void> receive(@RequestBody byte[] rawBody,
                                        @RequestHeader(value = "X-Signature", required = false) String signature) {
        paymentService.handleNotification(rawBody, signature);
        return ResponseEntity.noContent().build();
    }
}
