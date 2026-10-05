package ru.oldzoomer.geosearch.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Конфигурация REST-клиента для Overpass API.
 */
@Configuration
public class OverpassApiClientConfig {

    @Bean("overpassRestClient") RestClient overpassRestClient(OverpassApiConfig config) {
        return RestClient.builder()
                .baseUrl(config.getBaseUrl())
                .requestFactory(createRequestFactory(config))
                .defaultHeader("Accept", "application/json")
                .build();
    }

    private ClientHttpRequestFactory createRequestFactory(OverpassApiConfig config) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) config.getTimeout().toMillis());
        factory.setReadTimeout((int) config.getTimeout().toMillis());
        return factory;
    }
}
