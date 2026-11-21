package dev.cotai.proxy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;

@RestController
@RequestMapping("/api/proxy")
public class ProxyController {
    private final boolean enabled;
    private final String host;
    private final int port;
    private final String username;

    public ProxyController(
            @Value("${cotai.proxy.enabled:false}") boolean enabled,
            @Value("${cotai.proxy.host:127.0.0.1}") String host,
            @Value("${cotai.proxy.port:3128}") int port,
            @Value("${cotai.proxy.username:cotai_proxy}") String username) {
        this.enabled = enabled;
        this.host = host;
        this.port = port;
        this.username = username;
    }

    @GetMapping("/status")
    public ResponseEntity<ProxyStatusResponse> status() {
        boolean reachable = enabled && canConnect();
        String message = !enabled ? "Proxy desativado na configuração."
                : reachable ? "Servidor proxy disponível."
                : "Não foi possível conectar ao Squid. Verifique se o Docker está em execução.";
        ProxyStatusResponse response = new ProxyStatusResponse(
                enabled, reachable, host, port, username, true,
                "Somente requisições externas iniciadas pelo backend do COTAI", message);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }

    private boolean canConnect() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), (int) Duration.ofSeconds(2).toMillis());
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
