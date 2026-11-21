package dev.cotai.proxy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;

@Configuration(proxyBeanMethods = false)
public class ProxyConfiguration {

    @Bean(name = "cotaiExternalHttpClient")
    HttpClient cotaiExternalHttpClient(
            @Value("${cotai.proxy.enabled:false}") boolean enabled,
            @Value("${cotai.proxy.host:127.0.0.1}") String host,
            @Value("${cotai.proxy.port:3128}") int port,
            @Value("${cotai.proxy.username:cotai_proxy}") String username,
            @Value("${cotai.proxy.password:}") String password) {

        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10));

        if (enabled) {
            if (password.isBlank()) {
                throw new IllegalStateException("Configure PROXY_PASSWORD para habilitar o proxy externo.");
            }
            builder.proxy(ProxySelector.of(new InetSocketAddress(host, port)))
                    .authenticator(new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            if (getRequestorType() != RequestorType.PROXY) return null;
                            return new PasswordAuthentication(username, password.toCharArray());
                        }
                    });
        }
        return builder.build();
    }

    @Bean(name = "cotaiExternalRestClient")
    RestClient cotaiExternalRestClient(RestClient.Builder builder,
                                       @org.springframework.beans.factory.annotation.Qualifier("cotaiExternalHttpClient") HttpClient client) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(client);
        requestFactory.setReadTimeout(Duration.ofSeconds(30));
        return builder.requestFactory(requestFactory).build();
    }

    @Bean
public RestClient.Builder restClientBuilder() {
    return RestClient.builder();
}
}
