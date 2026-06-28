package io.github.mohamedmedhat21.order_processing_system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long expirationMs, long refreshExpirationMs) {
}
