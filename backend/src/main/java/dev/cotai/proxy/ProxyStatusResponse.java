package dev.cotai.proxy;

public record ProxyStatusResponse(
        boolean enabled,
        boolean reachable,
        String host,
        int port,
        String username,
        boolean authenticationRequired,
        String scope,
        String message) {
}
