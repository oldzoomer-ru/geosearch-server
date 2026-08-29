package ru.oldzoomer.geosearch.server.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.oldzoomer.geosearch.server.dto.PoiCategory;
import ru.oldzoomer.geosearch.server.dto.PoiDto;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PoiServiceTest {

    @Mock
    private OverpassApiClient overpassApiClient;

    private PoiService poiService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        poiService = new PoiService(overpassApiClient, objectMapper);
    }

    @Test
    @DisplayName("testGetPoisNear_WithValidCoordinates_ReturnsPoisList")
    void testGetPoisNear_WithValidCoordinates_ReturnsPoisList() {
        String mockResponse = """
                {
                  "elements": [
                    {
                      "type": "node",
                      "id": 100,
                      "lat": 55.7558,
                      "lon": 37.6173,
                      "tags": {
                        "name": "Кафе Пушкин",
                        "amenity": "CAFE"
                      }
                    }
                  ]
                }
                """;

        when(overpassApiClient.query(anyString())).thenReturn(mockResponse);

        List<PoiDto> pois = poiService.getPoisNear(55.7558, 37.6173, 1.0, PoiCategory.CAFE);

        assertEquals(1, pois.size());
        PoiDto poi = pois.getFirst();
        assertEquals("100", poi.getId());
        assertEquals("Кафе Пушкин", poi.getName());
        assertEquals(PoiCategory.CAFE, poi.getCategory());
        assertEquals(55.7558, poi.getLocation().getLat());
        assertEquals(37.6173, poi.getLocation().getLon());
    }

    @Test
    @DisplayName("testGetPoisNear_WithEmptyResponse_ReturnsEmptyList")
    void testGetPoisNear_WithEmptyResponse_ReturnsEmptyList() {
        String mockResponse = """
                {
                  "elements": []
                }
                """;

        when(overpassApiClient.query(anyString())).thenReturn(mockResponse);

        List<PoiDto> pois = poiService.getPoisNear(55.7558, 37.6173, 1.0, PoiCategory.RESTAURANT);

        assertTrue(pois.isEmpty());
    }

    @Test
    @DisplayName("testGetPoisNear_WithInvalidJson_ReturnsEmptyList")
    void testGetPoisNear_WithInvalidJson_ReturnsEmptyList() {
        when(overpassApiClient.query(anyString())).thenReturn("invalid json");

        List<PoiDto> pois = poiService.getPoisNear(55.7558, 37.6173, 1.0, PoiCategory.SHOP);

        assertTrue(pois.isEmpty());
    }

    @ParameterizedTest
    @EnumSource(value = PoiCategory.class, names = {"RESTAURANT", "CAFE", "HOSPITAL", "SCHOOL", "FUEL", "BANK", "PHARMACY"})
    @DisplayName("testGetPoisNear_WithAmenityCategories_CorrectlyFiltersByCategory")
    void testGetPoisNear_WithAmenityCategories_CorrectlyFiltersByCategory(PoiCategory category) {
        String tagValue = category.name();
        String mockResponse = """
                {
                  "elements": [
                    {
                      "type": "node",
                      "id": 200,
                      "lat": 55.76,
                      "lon": 37.62,
                      "tags": {
                        "name": "Тестовая точка",
                        "amenity": "%s"
                      }
                    }
                  ]
                }
                """.formatted(tagValue);

        when(overpassApiClient.query(anyString())).thenReturn(mockResponse);

        List<PoiDto> pois = poiService.getPoisNear(55.76, 37.62, 1.0, category);

        assertEquals(1, pois.size());
        assertEquals(category, pois.getFirst().getCategory());
    }

    @Test
    @DisplayName("testGetPoisNear_WithNoCategories_ReturnsAllPois")
    void testGetPoisNear_WithNoCategories_ReturnsAllPois() {
        String mockResponse = """
                {
                  "elements": [
                    {
                      "type": "node",
                      "id": 300,
                      "lat": 55.77,
                      "lon": 37.63,
                      "tags": {
                        "name": "Ресторан Тест",
                        "amenity": "RESTAURANT"
                      }
                    }
                  ]
                }
                """;

        when(overpassApiClient.query(anyString())).thenReturn(mockResponse);

        List<PoiDto> pois = poiService.getPoisNear(55.77, 37.63, 2.0);

        assertEquals(1, pois.size());
        assertEquals("300", pois.getFirst().getId());
        assertEquals(PoiCategory.RESTAURANT, pois.getFirst().getCategory());
    }

    @Test
    @DisplayName("testGetPoisNear_WithAddress_ExtractsAddressFields")
    void testGetPoisNear_WithAddress_ExtractsAddressFields() {
        String mockResponse = """
                {
                  "elements": [
                    {
                      "type": "node",
                      "id": 400,
                      "lat": 55.75,
                      "lon": 37.61,
                      "tags": {
                        "name": "Аптека №1",
                        "amenity": "PHARMACY",
                        "street": "Тестовая",
                        "housenumber": "15",
                        "city": "Москва",
                        "postcode": "101000"
                      }
                    }
                  ]
                }
                """;

        when(overpassApiClient.query(anyString())).thenReturn(mockResponse);

        List<PoiDto> pois = poiService.getPoisNear(55.75, 37.61, 1.0, PoiCategory.PHARMACY);

        assertEquals(1, pois.size());
        PoiDto poi = pois.getFirst();
        assertEquals("Тестовая, 15, Москва, 101000", poi.getAddress());
        assertNotNull(poi.getTags());
    }

    @Test
    @DisplayName("testGetPoisNear_WithTags_ExtractsExtraTags")
    void testGetPoisNear_WithTags_ExtractsExtraTags() {
        String mockResponse = """
                {
                  "elements": [
                    {
                      "type": "node",
                      "id": 500,
                      "lat": 55.75,
                      "lon": 37.61,
                      "tags": {
                        "name": "Банк Тест",
                        "amenity": "BANK",
                        "name:en": "Test Bank",
                        "opening_hours": "09:00-18:00"
                      }
                    }
                  ]
                }
                """;

        when(overpassApiClient.query(anyString())).thenReturn(mockResponse);

        List<PoiDto> pois = poiService.getPoisNear(55.75, 37.61, 1.0, PoiCategory.BANK);

        assertEquals(1, pois.size());
        PoiDto poi = pois.getFirst();
        assertEquals(2, poi.getTags().size());
        assertEquals("Test Bank", poi.getTags().get("name:en"));
        assertEquals("09:00-18:00", poi.getTags().get("opening_hours"));
    }
}
