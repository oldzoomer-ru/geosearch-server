package ru.oldzoomer.geosearch.server.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.oldzoomer.geosearch.server.dto.TransportRouteDto;
import ru.oldzoomer.geosearch.server.dto.TransportStopDto;
import ru.oldzoomer.geosearch.server.service.OverpassApiClient;
import ru.oldzoomer.geosearch.server.service.TransportService;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TransportControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TransportService transportService;

    @Mock
    private OverpassApiClient overpassApiClient;

    @BeforeEach
    void setUp() {
        TransportController controller = new TransportController(transportService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("testGetStopsNear_WithValidParams_ReturnsOkWithStops")
    void testGetStopsNear_WithValidParams_ReturnsOkWithStops() throws Exception {
        TransportStopDto stop = TransportStopDto.builder()
                .id("12345")
                .name("Красная площадь")
                .location(ru.oldzoomer.geosearch.server.dto.GeoLocation.builder()
                        .lat(55.7558).lon(37.6173).build())
                .operator("Московский Транспорт")
                .routes(List.of(
                        TransportStopDto.RouteInfo.builder().id("111").name("1").build(),
                        TransportStopDto.RouteInfo.builder().id("222").name("2").build()
                ))
                .build();

        when(transportService.getStopsNear(55.7558, 37.6173, 1.0))
                .thenReturn(List.of(stop));

        mockMvc.perform(get("/api/v1/transport/stops")
                        .param("lat", "55.7558")
                        .param("lon", "37.6173")
                        .param("radiusKm", "1.0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("12345"))
                .andExpect(jsonPath("$[0].name").value("Красная площадь"))
                .andExpect(jsonPath("$[0].location.lat").value(55.7558))
                .andExpect(jsonPath("$[0].location.lon").value(37.6173))
                .andExpect(jsonPath("$[0].operator").value("Московский Транспорт"));
    }

    @Test
    @DisplayName("testGetStopsNear_WithEmptyResult_ReturnsOkWithEmptyArray")
    void testGetStopsNear_WithEmptyResult_ReturnsOkWithEmptyArray() throws Exception {
        when(transportService.getStopsNear(anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/transport/stops")
                        .param("lat", "55.7558")
                        .param("lon", "37.6173")
                        .param("radiusKm", "1.0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("testGetRoute_WithValidRouteId_ReturnsOkWithRoute")
    void testGetRoute_WithValidRouteId_ReturnsOkWithRoute() throws Exception {
        TransportRouteDto route = TransportRouteDto.builder()
                .id("111")
                .name("Маршрут 1")
                .type("bus")
                .stops(List.of("123", "456"))
                .build();

        when(transportService.getRoute("111")).thenReturn(route);

        mockMvc.perform(get("/api/v1/transport/routes/111")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("111"))
                .andExpect(jsonPath("$.name").value("Маршрут 1"))
                .andExpect(jsonPath("$.type").value("bus"))
                .andExpect(jsonPath("$.stops.length()").value(2));
    }

    @Test
    @DisplayName("testGetRoute_WithNotFoundRouteId_ReturnsNotFound")
    void testGetRoute_WithNotFoundRouteId_ReturnsNotFound() throws Exception {
        when(transportService.getRoute("999")).thenReturn(null);

        mockMvc.perform(get("/api/v1/transport/routes/999")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("testGetStopsNear_WithDefaultRadius_ReturnsOkWithStops")
    void testGetStopsNear_WithDefaultRadius_ReturnsOkWithStops() throws Exception {
        TransportStopDto stop = TransportStopDto.builder()
                .id("999")
                .name("Тестовая остановка")
                .location(ru.oldzoomer.geosearch.server.dto.GeoLocation.builder()
                        .lat(55.76).lon(37.62).build())
                .build();

        when(transportService.getStopsNear(55.76, 37.62, 1.0))
                .thenReturn(List.of(stop));

        mockMvc.perform(get("/api/v1/transport/stops")
                        .param("lat", "55.76")
                        .param("lon", "37.62"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("999"));
    }
}
