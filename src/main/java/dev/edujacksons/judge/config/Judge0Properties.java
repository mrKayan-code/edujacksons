package dev.edujacksons.judge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Настройки клиента Judge0. {@code baseUrl} — адрес self-hosted Judge0 (см. docker-compose,
 * профиль judge0). Дефолт — локальный, только для разработки.
 */
@ConfigurationProperties(prefix = "app.judge0")
public record Judge0Properties(
        String baseUrl,
        Integer connectTimeoutMs,
        Integer readTimeoutMs
) {
    public Judge0Properties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:2358";
        }
        if (connectTimeoutMs == null) {
            connectTimeoutMs = 2000;
        }
        if (readTimeoutMs == null) {
            readTimeoutMs = 15000;
        }
    }
}
