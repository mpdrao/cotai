package dev.cotai.mockgateway;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import dev.cotai.common.BusinessException;
import dev.cotai.common.ErrorCode;
import dev.cotai.mockgateway.MockPaymentGateway.ChargeStatus;
import dev.cotai.mockgateway.MockPaymentGateway.MockCharge;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.Base64;

/** Checkout page + fake provider API (dev profile only). Rendered by Spring, not by the SPA, on purpose. */
@Profile("dev")
@Controller
@RequestMapping("/mock-gateway")
@RequiredArgsConstructor
public class MockGatewayController {

    public record MockChargeResponse(String providerReference, ChargeStatus status, long amountCents,
                                     Instant expiresAt, String checkoutUrl) {
    }

    private final MockPaymentGateway gateway;

    @GetMapping("/checkout/{providerReference}")
    public String checkout(@PathVariable String providerReference, Model model) {
        MockCharge charge = charge(providerReference);
        model.addAttribute("charge", charge);
        model.addAttribute("amount", String.format("R$ %,.2f", charge.amountCents() / 100.0));
        model.addAttribute("qrDataUri", qrDataUri(charge.checkoutUrl()));
        model.addAttribute("expired", gateway.isExpired(charge));
        return "mock-checkout";
    }

    @GetMapping("/charges/{providerReference}")
    @ResponseBody
    public MockChargeResponse get(@PathVariable String providerReference) {
        MockCharge c = charge(providerReference);
        return new MockChargeResponse(c.providerReference(), c.status(), c.amountCents(), c.expiresAt(),
                c.checkoutUrl());
    }

    @PostMapping("/charges/{providerReference}/approve")
    @ResponseBody
    public ResponseEntity<Void> approve(@PathVariable String providerReference) {
        MockCharge c = pendingCharge(providerReference);
        gateway.approve(c);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/charges/{providerReference}/decline")
    @ResponseBody
    public ResponseEntity<Void> decline(@PathVariable String providerReference) {
        MockCharge c = pendingCharge(providerReference);
        gateway.decline(c);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    private MockCharge pendingCharge(String providerReference) {
        MockCharge c = charge(providerReference);
        if (c.status() != ChargeStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "Charge is not PENDING.");
        }
        if (gateway.isExpired(c)) {
            throw new BusinessException(ErrorCode.CHARGE_EXPIRED, "Charge expired.");
        }
        return c;
    }

    private MockCharge charge(String providerReference) {
        return gateway.find(providerReference).orElseThrow(() -> BusinessException.notFound("Charge"));
    }

    private static String qrDataUri(String content) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 260, 260);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Could not render QR code", e);
        }
    }
}
