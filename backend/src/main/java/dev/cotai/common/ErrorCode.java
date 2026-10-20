package dev.cotai.common;

import org.springframework.http.HttpStatus;

/**
 * Stable error codes returned in the {@code code} field of RFC 9457 Problem Details.
 * Mirrors {@code ErrorCode} in docs/api/openapi.yaml.
 */
public enum ErrorCode {

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Validation failed"),
    EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "E-mail already registered"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid credentials"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Not found"),
    EVENT_DATE_IN_PAST(HttpStatus.UNPROCESSABLE_CONTENT, "Event date in the past"),
    LIST_HAS_NO_ITEMS(HttpStatus.UNPROCESSABLE_CONTENT, "Gift list has no items"),
    LIST_NOT_ACCEPTING_RESERVATIONS(HttpStatus.CONFLICT, "Gift list is not accepting reservations"),
    QUOTA_UNAVAILABLE(HttpStatus.CONFLICT, "Quota unavailable"),
    QUOTAS_BELOW_OCCUPIED(HttpStatus.CONFLICT, "Quotas below occupied"),
    ITEM_HAS_ACTIVE_RESERVATIONS(HttpStatus.CONFLICT, "Item has active reservations"),
    INVALID_STATE_TRANSITION(HttpStatus.CONFLICT, "Invalid state transition"),
    IDEMPOTENCY_KEY_MISMATCH(HttpStatus.UNPROCESSABLE_CONTENT, "Idempotency key mismatch"),
    TOO_MANY_PENDING_RESERVATIONS(HttpStatus.TOO_MANY_REQUESTS, "Too many pending reservations"),
    PAYMENT_GATEWAY_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "Payment gateway unavailable"),
    INVALID_SIGNATURE(HttpStatus.UNAUTHORIZED, "Invalid signature"),
    CHARGE_EXPIRED(HttpStatus.GONE, "Charge expired"),
    PHOTO_INVALID(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Photo format not supported"),
    PHOTO_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "Photo too large");

    private final HttpStatus status;
    private final String title;

    ErrorCode(HttpStatus status, String title) {
        this.status = status;
        this.title = title;
    }

    public HttpStatus status() {
        return status;
    }

    public String title() {
        return title;
    }

    /** Slug used in the problem {@code type} URI, e.g. {@code quota-unavailable}. */
    public String slug() {
        return name().toLowerCase().replace('_', '-');
    }
}
