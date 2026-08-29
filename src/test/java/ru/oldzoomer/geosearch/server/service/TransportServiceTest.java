package ru.oldzoomer.geosearch.server.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.oldzoomer.geosearch.server.dto.TransportRouteDto;
import ru.oldzoomer.geosearch.server.dto.TransportStopDto;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransportServiceTest {

    @Mock
    private OverpassApiClient overpassApiClient;

    private TransportService transportService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        transportService = new TransportService(overpassApiClient, objectMapper);
    }

    @Test
    @DisplayName("testGetStopsNear_WithValidCoordinates_ReturnsStopsList")
    void testGetStopsNear_WithValidCoordinates_ReturnsStopsList() {
        String mockResponse = """
                {
                  "elements": [
                    {
                      "type": "node",
                      "id": 12345,
                      "lat": 55.7558,
                      "lon": 37.6173,
                      "tags": {
                        "name": "Красная площадь",
                        "highway": "bus_stop"
                      }
                    }
                  ]
                }
                """;

        when(overpassApiClient.query(anyString())).thenReturn(mockResponse);

        List<TransportStopDto> stops = transportService.getStopsNear(55.7558, 37.6173, 1.0);

        assertEquals(1, stops.size());
        TransportStopDto stop = stops.getFirst();
        assertEquals("12345", stop.getId());
        assertEquals("Красная площадь", stop.getName());
        assertEquals(55.7558, stop.getLocation().getLat());
        assertEquals(37.6173, stop.getLocation().getLon());
        assertNotNull(stop.getRoutes());
    }

    @Test
    @DisplayName("testGetStopsNear_WithEmptyResponse_ReturnsEmptyList")
    void testGetStopsNear_WithEmptyResponse_ReturnsEmptyList() {
        String mockResponse = """
                {
                  "elements": []
                }
                """;

        when(overpassApiClient.query(anyString())).thenReturn(mockResponse);

        List<TransportStopDto> stops = transportService.getStopsNear(55.7558, 37.6173, 1.0);

        assertTrue(stops.isEmpty());
    }

    @Test
    @DisplayName("testGetStopsNear_WithInvalidJson_ReturnsEmptyList")
    void testGetStopsNear_WithInvalidJson_ReturnsEmptyList() {
        when(overpassApiClient.query(anyString())).thenReturn("invalid json");

        List<TransportStopDto> stops = transportService.getStopsNear(55.7558, 37.6173, 1.0);

        assertTrue(stops.isEmpty());
    }

    @Test
    @DisplayName("testGetStopsNear_WithWayElement_UsesCenterCoordinates")
    void testGetStopsNear_WithWayElement_UsesCenterCoordinates() {
        String mockResponse = """
                {
                  "elements": [
                    {
                      "type": "way",
                      "id": 67890,
                      "center": {
                        "lat": 55.7512,
                        "lon": 37.6184
                      },
                      "tags": {
                        "name": "Остановка №5"
                      }
                    }
                  ]
                }
                """;

        when(overpassApiClient.query(anyString())).thenReturn(mockResponse);

        List<TransportStopDto> stops = transportService.getStopsNear(55.7558, 37.6173, 1.0);

        assertEquals(1, stops.size());
        TransportStopDto stop = stops.getFirst();
        assertEquals("67890", stop.getId());
        assertEquals(55.7512, stop.getLocation().getLat());
        assertEquals(37.6184, stop.getLocation().getLon());
    }

    @Test
    @DisplayName("testGetRoute_WithValidRouteId_ReturnsRoute")
    void testGetRoute_WithValidRouteId_ReturnsRoute() {
        String mockResponse = """
                {
                  "elements": [
                    {
                      "type": "relation",
                      "id": 111,
                      "members": [
                        {
                          "type": "node",
                          "ref": 123,
                          "role": "stop_position"
                        },
                        {
                          "type": "node",
                          "ref": 456,
                          "role": "stop_position"
                        }
                      ],
                      "tags": {
                        "name": "Маршрут 1",
                        "route": "bus"
                      }
                    }
                  ]
                }
                """;

        when(overpassApiClient.query(anyString())).thenReturn(mockResponse);

        TransportRouteDto route = transportService.getRoute("111");

        assertNotNull(route);
        assertEquals("111", route.getId());
        assertEquals("Маршрут 1", route.getName());
        assertEquals("bus", route.getType());
        assertEquals(2, route.getStops().size());
        assertTrue(route.getStops().contains("123"));
        assertTrue(route.getStops().contains("456"));
    }

    @Test
    @DisplayName("testGetRoute_WithNotFoundRouteId_ReturnsNull")
    void testGetRoute_WithNotFoundRouteId_ReturnsNull() {
        String mockResponse = """
                {
                  "elements": []
                }
                """;

        when(overpassApiClient.query(anyString())).thenReturn(mockResponse);

        TransportRouteDto route = transportService.getRoute("999");

        assertNull(route);
    }

    @Test
    @DisplayName("testGetStopsNear_WithOperator_ExtractsOperatorName")
    void testGetStopsNear_WithOperator_ExtractsOperatorName() {
        String mockResponse = """
                {
                  "elements": [
                    {
                      "type": "node",
                      "id": 555,
                      "lat": 55.76,
                      "lon": 37.62,
                      "tags": {
                        "name": "Центральная",
                        "operator": "Московский Транспорт"
                      }
                    }
                  ]
                }
                """;

        when(overpassApiClient.query(anyString())).thenReturn(mockResponse);

        List<TransportStopDto> stops = transportService.getStopsNear(55.76, 37.62, 2.0);

        assertEquals(1, stops.size());
        assertEquals("Московский Транспорт", stops.getFirst().getOperator());
    }
}
