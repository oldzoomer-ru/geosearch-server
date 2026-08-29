package ru.oldzoomer.geosearch.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ru.oldzoomer.geosearch.server.dto.GeoLocation;
import ru.oldzoomer.geosearch.server.dto.TransportRouteDto;
import ru.oldzoomer.geosearch.server.dto.TransportStopDto;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Сервис для работы с данными общественного транспорта.
 * <p>
 * Запрашивает данные из Overpass API, парсит ответы и предоставляет
 * информацию об остановках и маршрутах в заданной области.
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class TransportService {

    private static final String STOPS_QUERY_TEMPLATE = """
            [out:json][timeout:30];
            (
              node["highway"="bus_stop"](%s,%s,%s,%s);
              way["highway"="bus_stop"](%s,%s,%s,%s);
            );
            out center;
            """;

    private final OverpassApiClient overpassApiClient;
    private final ObjectMapper objectMapper;

    /**
     * Находит остановки общественного транспорта в заданном радиусе.
     *
     * @param lat      широта центра области
     * @param lon      долгота центра области
     * @param radiusKm радиус поиска в километрах
     * @return список найденных остановок
     */
    @Cacheable(value = "transport-stops")
    public List<TransportStopDto> getStopsNear(double lat, double lon, double radiusKm) {
        log.info("Searching for transport stops near ({}, {}) with radius {} km", lat, lon, radiusKm);

        double[] bbox = calculateBbox(lat, lon, radiusKm);
        String query = String.format(STOPS_QUERY_TEMPLATE,
                bbox[1], bbox[0], bbox[3], bbox[2],
                bbox[1], bbox[0], bbox[3], bbox[2]);

        String response = overpassApiClient.query(query);
        return parseStops(response);
    }

    /**
     * Получает информацию о маршруте по его идентификатору.
     *
     * @param routeId идентификатор маршрута в OSM
     * @return DTO маршрута или null если не найден
     */
    @Cacheable(value = "routes")
    public TransportRouteDto getRoute(String routeId) {
        log.info("Fetching route with id: {}", routeId);

        String query = String.format("""
                [out:json][timeout:30];
                rel[%s](%s);
                out body;
                """, "route", routeId);

        String response = overpassApiClient.query(query);
        return parseRoute(response, routeId);
    }

    /**
     * Вычисляет bounding box вокруг точки заданного радиуса.
     */
    private double[] calculateBbox(double lat, double lon, double radiusKm) {
        final double earthRadius = 6371;
        double radiusDeg = radiusKm / earthRadius * 180 / Math.PI;
        double lonDeg = radiusDeg / Math.cos(Math.toRadians(lat));

        return new double[]{
                lon - lonDeg, lat - radiusDeg,
                lon + lonDeg, lat + radiusDeg
        };
    }

    /**
     * Парсит JSON-ответ и извлекает остановки.
     */
    private List<TransportStopDto> parseStops(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode elements = root.path("elements");

            List<TransportStopDto> stops = new ArrayList<>();
            for (JsonNode element : elements) {
                String type = element.path("type").asString();
                long id = element.path("id").asLong();

                if (!"node".equals(type) && !"way".equals(type)) {
                    continue;
                }

                String name = element.path("tags").path("name").asString(null);
                if (name == null) {
                    name = element.path("tags").path("ref").asString(null);
                }
                if (name == null) {
                    name = "Stop-" + id;
                }

                GeoLocation location = extractLocation(element, type);
                if (location == null) {
                    continue;
                }

                String operator = element.path("tags").path("operator").asString(null);

                stops.add(TransportStopDto.builder()
                        .id(String.valueOf(id))
                        .name(name)
                        .location(location)
                        .operator(operator)
                        .routes(new ArrayList<>())
                        .build());
            }

            log.info("Found {} transport stops", stops.size());
            return stops;

        } catch (Exception e) {
            log.error("Failed to parse transport stops response: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Извлекает координаты из элемента OSM (node или way с center).
     */
    private GeoLocation extractLocation(JsonNode element, String type) {
        if ("node".equals(type)) {
            double lat = element.path("lat").asDouble(0);
            double lon = element.path("lon").asDouble(0);
            if (lat != 0 && lon != 0) {
                return GeoLocation.builder().lat(lat).lon(lon).build();
            }
        } else if ("way".equals(type)) {
            JsonNode center = element.path("center");
            if (!center.isMissingNode()) {
                double lat = center.path("lat").asDouble(0);
                double lon = center.path("lon").asDouble(0);
                if (lat != 0 && lon != 0) {
                    return GeoLocation.builder().lat(lat).lon(lon).build();
                }
            }
        }
        return null;
    }

    /**
     * Парсит JSON-ответ и извлекает маршрут.
     */
    private TransportRouteDto parseRoute(String jsonResponse, String routeId) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode elements = root.path("elements");

            if (elements.isEmpty()) {
                return null;
            }

            JsonNode rel = elements.get(0);
            JsonNode tags = rel.path("tags");

            String name = tags.path("name").asString("Route-" + routeId);
            String type = tags.path("route").asString("bus");
            if (tags.has("public_transport")) {
                type = tags.path("public_transport").asString(type);
            }

            Set<String> stopIds = new HashSet<>();
            JsonNode members = rel.path("members");
            for (JsonNode member : members) {
                if ("node".equals(member.path("type").asString())
                        && "stop_position".equals(member.path("role").asString())) {
                    stopIds.add(String.valueOf(member.path("ref").asLong()));
                }
            }

            return TransportRouteDto.builder()
                    .id(routeId)
                    .name(name)
                    .type(type)
                    .stops(new ArrayList<>(stopIds))
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse route response: {}", e.getMessage(), e);
            return null;
        }
    }
}
