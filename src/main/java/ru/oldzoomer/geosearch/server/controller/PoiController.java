package ru.oldzoomer.geosearch.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.oldzoomer.geosearch.server.dto.PoiCategory;
import ru.oldzoomer.geosearch.server.dto.PoiDto;
import ru.oldzoomer.geosearch.server.service.PoiService;

import java.util.List;

/**
 * REST-контроллер для работы с точками интереса (POI).
 */
@RestController
@RequestMapping("/api/v1/pois")
@Validated
@Tag(name = "POI API", description = "API для работы с точками интереса (POI)")
@RequiredArgsConstructor
public class PoiController {

    private final PoiService poiService;

    /**
     * Находит точки интереса в заданной области с возможностью фильтрации по категориям.
     *
     * @param lat        широта центра области
     * @param lon        долгота центра области
     * @param radiusKm   радиус поиска в километрах (по умолчанию 1 км)
     * @param categories категории POI для фильтрации (необязательно)
     * @return список найденных точек интереса
     */
    @GetMapping
    @Operation(
            summary = "Найти точки интереса",
            description = "Возвращает список точек интереса (POI) в заданном радиусе от указанных координат с возможностью фильтрации по категориям"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Точки интереса успешно найдены"),
            @ApiResponse(responseCode = "400", description = "Некорректные параметры запроса")
    })
    public ResponseEntity<List<PoiDto>> getPoisNear(
            @Parameter(description = "Широта центра области", required = true, example = "55.7558")
            @RequestParam @Positive double lat,

            @Parameter(description = "Долгота центра области", required = true, example = "37.6173")
            @RequestParam @Positive double lon,

            @Parameter(description = "Радиус поиска в км", example = "1.0")
            @RequestParam(defaultValue = "1.0") @Positive double radiusKm,

            @Parameter(description = "Категории POI для фильтрации (необязательно)")
            @RequestParam(required = false) PoiCategory[] categories) {

        List<PoiDto> pois = poiService.getPoisNear(lat, lon, radiusKm, categories);
        return ResponseEntity.ok(pois);
    }
}
