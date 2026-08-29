package ru.oldzoomer.geosearch.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ru.oldzoomer.geosearch.server.dto.GeoLocation;
import ru.oldzoomer.geosearch.server.dto.PoiCategory;
import ru.oldzoomer.geosearch.server.dto.PoiDto;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.*;

/**
 * Сервис для работы с точками интереса (POI).
 * <p>
 * Запрашивает данные из Overpass API и предоставляет информацию
 * о точках интереса в заданной области с возможностью фильтрации по категориям.
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class PoiService {

    private static final Map<PoiCategory, String> CATEGORY_TO_OSM_MAP = Map.of(
            PoiCategory.RESTAURANT, "amenity=RESTAURANT",
            PoiCategory.CAFE, "amenity=CAFE",
            PoiCategory.SHOP, "SHOP=*",
            PoiCategory.HOSPITAL, "amenity=HOSPITAL",
            PoiCategory.SCHOOL, "amenity=SCHOOL",
            PoiCategory.PARK, "leisure=PARK",
            PoiCategory.HOTEL, "tourism=HOTEL",
            PoiCategory.FUEL, "amenity=FUEL",
            PoiCategory.BANK, "amenity=BANK",
            PoiCategory.PHARMACY, "amenity=PHARMACY"
    );

    private final OverpassApiClient overpassApiClient;
    private final ObjectMapper objectMapper;

    /**
     * Находит точки интереса в заданном радиусе с фильтрацией по категориям.
     *
     * @param lat        широта центра области
     * @param lon        долгота центра области
     * @param radiusKm   радиус поиска в километрах
     * @param categories категории POI для фильтрации (если null или пустой — возвращаются все)
     * @return список найденных точек интереса
     */
    @Cacheable(value = "poi")
    public List<PoiDto> getPoisNear(double lat, double lon, double radiusKm, PoiCategory... categories) {
        log.info("Searching for POIs near ({}, {}) with radius {} km, categories: {}",
                lat, lon, radiusKm, Arrays.toString(categories));

        double[] bbox = calculateBbox(lat, lon, radiusKm);
        String query = buildPoiQuery(bbox, categories);

        String response = overpassApiClient.query(query);
        return parsePois(response);
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
     * Строит Overpass-запрос для POI.
     */
    private String buildPoiQuery(double[] bbox, PoiCategory[] categories) {
        StringBuilder query = new StringBuilder();
        query.append("[out:json][timeout:30];\n");
        query.append("(\n");

        if (categories == null || categories.length == 0) {
            query.append("  node[").append("amenity=").append("*").append("](")
                    .append(bbox[1]).append(",").append(bbox[0]).append(",").append(bbox[3]).append(",").append(bbox[2]).append(");\n");
            query.append("  node[").append("SHOP=").append("*").append("](")
                    .append(bbox[1]).append(",").append(bbox[0]).append(",").append(bbox[3]).append(",").append(bbox[2]).append(");\n");
            query.append("  node[").append("tourism=").append("*").append("](")
                    .append(bbox[1]).append(",").append(bbox[0]).append(",").append(bbox[3]).append(",").append(bbox[2]).append(");\n");
            query.append("  node[").append("leisure=").append("*").append("](")
                    .append(bbox[1]).append(",").append(bbox[0]).append(",").append(bbox[3]).append(",").append(bbox[2]).append(");\n");
        } else {
            for (PoiCategory category : categories) {
                String osmFilter = CATEGORY_TO_OSM_MAP.get(category);
                if (osmFilter != null) {
                    query.append("  node[\"").append(osmFilter).append("\"](")
                            .append(bbox[1]).append(",").append(bbox[0]).append(",").append(bbox[3]).append(",").append(bbox[2]).append(");\n");
                }
            }
        }

        query.append(");\n");
        query.append("out body;");
        return query.toString();
    }

    /**
     * Парсит JSON-ответ и извлекает точки интереса.
     */
    private List<PoiDto> parsePois(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode elements = root.path("elements");

            List<PoiDto> pois = new ArrayList<>();
            for (JsonNode element : elements) {
                String type = element.path("type").asString();
                if (!"node".equals(type)) {
                    continue;
                }

                long id = element.path("id").asLong();
                JsonNode tags = element.path("tags");

                PoiCategory category = extractCategory(tags);
                if (category == null) {
                    continue;
                }

                String name = tags.path("name").asString(null);
                if (name == null) {
                    name = tags.path("brand").asString(null);
                }
                if (name == null) {
                    name = "POI-" + id;
                }

                double lat = element.path("lat").asDouble(0);
                double lon = element.path("lon").asDouble(0);
                if (lat == 0 && lon == 0) {
                    continue;
                }

                GeoLocation location = GeoLocation.builder().lat(lat).lon(lon).build();

                String address = extractAddress(tags);
                Map<String, String> tagsMap = extractTags(tags);

                pois.add(PoiDto.builder()
                        .id(String.valueOf(id))
                        .name(name)
                        .category(category)
                        .location(location)
                        .tags(tagsMap)
                        .address(address)
                        .build());
            }

            log.info("Found {} POIs", pois.size());
            return pois;

        } catch (Exception e) {
            log.error("Failed to parse POIs response: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Определяет категорию POI по тегам OSM.
     */
    private PoiCategory extractCategory(JsonNode tags) {
        for (Map.Entry<PoiCategory, String> entry : CATEGORY_TO_OSM_MAP.entrySet()) {
            String filter = entry.getValue();
            if (filter.contains("=")) {
                String key = filter.split("=")[0];
                String value = filter.split("=")[1];
                if ("*".equals(value)) {
                    if (tags.has(key) && !tags.path(key).isNull()) {
                        return entry.getKey();
                    }
                } else {
                    String actualValue = tags.path(key).asString(null);
                    if (actualValue != null && actualValue.equalsIgnoreCase(value)) {
                        return entry.getKey();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Извлекает адрес из тегов OSM.
     */
    private String extractAddress(JsonNode tags) {
        String street = tags.path("street").asString(null);
        String house = tags.path("housenumber").asString(null);
        String city = tags.path("city").asString(null);
        String postcode = tags.path("postcode").asString(null);

        List<String> parts = new ArrayList<>();
        if (street != null) parts.add(street);
        if (house != null) parts.add(house);
        if (city != null) parts.add(city);
        if (postcode != null) parts.add(postcode);

        return String.join(", ", parts);
    }

    /**
     * Извлекает все теги OSM в виде Map.
     */
    private Map<String, String> extractTags(JsonNode tags) {
        Map<String, String> result = new HashMap<>();
        tags.properties().forEach(field -> {
            if (!field.getKey().equals("name")
                    && !field.getKey().equals("street")
                    && !field.getKey().equals("housenumber")
                    && !field.getKey().equals("city")
					&& !field.getKey().equals("postcode")
					&& !field.getKey().equals("amenity")) {
                result.put(field.getKey(), field.getValue().asString());
            }
        });
        return result;
    }
}
