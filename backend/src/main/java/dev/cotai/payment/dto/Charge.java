package dev.cotai.payment.dto;

public record Charge(String providerReference, String checkoutUrl, String qrCodePayload) {
}
