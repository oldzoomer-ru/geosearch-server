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
import ru.oldzoomer.geosearch.server.dto.GeoLocation;
import ru.oldzoomer.geosearch.server.dto.PoiCategory;
import ru.oldzoomer.geosearch.server.dto.PoiDto;
import ru.oldzoomer.geosearch.server.service.PoiService;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PoiControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PoiService poiService;

    @BeforeEach
    void setUp() {
        PoiController controller = new PoiController(poiService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("testGetPoisNear_WithValidParams_ReturnsOkWithPois")
    void testGetPoisNear_WithValidParams_ReturnsOkWithPois() throws Exception {
        PoiDto poi = PoiDto.builder()
                .id("100")
                .name("Кафе Пушкин")
                .category(PoiCategory.CAFE)
                .location(GeoLocation.builder().lat(55.7558).lon(37.6173).build())
                .tags(Map.of("cuisine", "russian"))
                .address("Тестовая ул., 1, Москва")
                .build();

        when(poiService.getPoisNear(eq(55.7558), eq(37.6173), eq(1.0), eq(PoiCategory.CAFE)))
                .thenReturn(List.of(poi));

        mockMvc.perform(get("/api/v1/pois")
                        .param("lat", "55.7558")
                        .param("lon", "37.6173")
                        .param("radiusKm", "1.0")
                        .param("categories", "CAFE")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("100"))
                .andExpect(jsonPath("$[0].name").value("Кафе Пушкин"))
                .andExpect(jsonPath("$[0].category").value("CAFE"))
                .andExpect(jsonPath("$[0].location.lat").value(55.7558))
                .andExpect(jsonPath("$[0].location.lon").value(37.6173))
                .andExpect(jsonPath("$[0].address").value("Тестовая ул., 1, Москва"));
    }

    @Test
    @DisplayName("testGetPoisNear_WithEmptyResult_ReturnsOkWithEmptyArray")
    void testGetPoisNear_WithEmptyResult_ReturnsOkWithEmptyArray() throws Exception {
        when(poiService.getPoisNear(anyDouble(), anyDouble(), anyDouble(), any(PoiCategory.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/pois")
                        .param("lat", "55.7558")
                        .param("lon", "37.6173")
                        .param("radiusKm", "1.0")
                        .param("categories", "RESTAURANT")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("testGetPoisNear_WithHotelCategory_ReturnsFilteredPois")
    void testGetPoisNear_WithHotelCategory_ReturnsFilteredPois() throws Exception {
        PoiDto poi = PoiDto.builder()
                .id("300")
                .name("Отель Тест")
                .category(PoiCategory.HOTEL)
                .location(GeoLocation.builder().lat(55.77).lon(37.63).build())
                .build();

        when(poiService.getPoisNear(eq(55.77), eq(37.63), eq(1.0), eq(PoiCategory.HOTEL)))
                .thenReturn(List.of(poi));

        mockMvc.perform(get("/api/v1/pois")
                        .param("lat", "55.77")
                        .param("lon", "37.63")
                        .param("radiusKm", "1.0")
                        .param("categories", "HOTEL")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].category").value("HOTEL"));
    }

    @Test
    @DisplayName("testGetPoisNear_WithDefaultRadius_ReturnsOkWithPois")
    void testGetPoisNear_WithDefaultRadius_ReturnsOkWithPois() throws Exception {
        PoiDto poi = PoiDto.builder()
                .id("400")
                .name("Парк Тест")
                .category(PoiCategory.PARK)
                .location(GeoLocation.builder().lat(55.78).lon(37.64).build())
                .build();

        when(poiService.getPoisNear(55.78, 37.64, 1.0, (PoiCategory[]) null))
                .thenReturn(List.of(poi));

        mockMvc.perform(get("/api/v1/pois")
                        .param("lat", "55.78")
                        .param("lon", "37.64"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("400"));
    }

    @Test
    @DisplayName("testGetPoisNear_WithNoCategories_ReturnsAllPois")
    void testGetPoisNear_WithNoCategories_ReturnsAllPois() throws Exception {
        PoiDto poi = PoiDto.builder()
                .id("200")
                .name("Ресторан Тест")
                .category(PoiCategory.RESTAURANT)
                .location(GeoLocation.builder().lat(55.76).lon(37.62).build())
                .build();

        when(poiService.getPoisNear(55.76, 37.62, 2.0, (PoiCategory[]) null))
                .thenReturn(List.of(poi));

        mockMvc.perform(get("/api/v1/pois")
                        .param("lat", "55.76")
                        .param("lon", "37.62")
                        .param("radiusKm", "2.0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("200"));
    }
}
