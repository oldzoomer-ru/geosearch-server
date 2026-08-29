package ru.oldzoomer.geosearch.server.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Конфигурация клиента Overpass API.
 */
@Data
@Validated
@Component
@ConfigurationProperties(prefix = "overpass")
public class OverpassApiConfig {

    /**
     * Базовый URL Overpass API.
     */
    @NotBlank
    private String baseUrl = "https://overpass-api.de/api/interpreter";

    /**
     * Таймаут HTTP-запросов.
     */
    private Duration timeout = Duration.ofSeconds(30);
}
