package ru.oldzoomer.geosearch.server.service;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * HTTP-клиент для Overpass API — основного источника данных OSM.
 * <p>
 * Выполняет запросы к Overpass API для получения данных об остановках,
 * маршрутах и точках интереса (POI).
 */
@Component
@Log4j2
public class OverpassApiClient {

    private final RestClient restClient;

    public OverpassApiClient(RestClient overpassApiClient) {
        this.restClient = overpassApiClient;
    }

    /**
     * Выполняет запрос к Overpass API и возвращает ответ в формате JSON.
     *
     * @param osmQuery OSM-запрос в формате Overpass QL
     * @return JSON-ответ от Overpass API
     * @throws RestClientException если запрос не удался
     */
    public String query(String osmQuery) {
        if (osmQuery == null || osmQuery.isBlank()) {
            throw new IllegalArgumentException("Overpass query must not be null or blank");
        }
        log.debug("Executing Overpass query: {}", osmQuery);
        try {
            return restClient.post()
                    .body(osmQuery)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            throw new RestClientException("Overpass API query failed: " + e.getMessage(), e);
        }
    }
}
