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
import org.springframework.web.bind.annotation.*;
import ru.oldzoomer.geosearch.server.dto.TransportRouteDto;
import ru.oldzoomer.geosearch.server.dto.TransportStopDto;
import ru.oldzoomer.geosearch.server.service.TransportService;

import java.util.List;

/**
 * REST-контроллер для работы с данными общественного транспорта.
 */
@RestController
@RequestMapping("/api/v1/transport")
@Validated
@Tag(name = "Transport API", description = "API для работы с данными общественного транспорта")
@RequiredArgsConstructor
public class TransportController {

    private final TransportService transportService;

    /**
     * Находит остановки общественного транспорта в заданной области.
     *
     * @param lat      широта центра области
     * @param lon      долгота центра области
     * @param radiusKm радиус поиска в километрах (по умолчанию 1 км)
     * @return список остановок в заданном радиусе
     */
    @GetMapping("/stops")
    @Operation(
            summary = "Найти остановки транспорта",
            description = "Возвращает список остановок общественного транспорта в заданном радиусе от указанных координат"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Остановки успешно найдены"),
            @ApiResponse(responseCode = "400", description = "Некорректные параметры запроса")
    })
    public ResponseEntity<List<TransportStopDto>> getStopsNear(
            @Parameter(description = "Широта центра области", required = true, example = "55.7558")
            @RequestParam @Positive double lat,

            @Parameter(description = "Долгота центра области", required = true, example = "37.6173")
            @RequestParam @Positive double lon,

            @Parameter(description = "Радиус поиска в км", example = "1.0")
            @RequestParam(defaultValue = "1.0") @Positive double radiusKm) {

        List<TransportStopDto> stops = transportService.getStopsNear(lat, lon, radiusKm);
        return ResponseEntity.ok(stops);
    }

    /**
     * Получает информацию о маршруте по его идентификатору.
     *
     * @param routeId идентификатор маршрута в OSM
     * @return DTO маршрута
     */
    @GetMapping("/routes/{routeId}")
    @Operation(
            summary = "Получить маршрут",
            description = "Возвращает информацию о маршруте общественного транспорта по его идентификатору"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Маршрут успешно найден"),
            @ApiResponse(responseCode = "404", description = "Маршрут не найден"),
            @ApiResponse(responseCode = "400", description = "Некорректный формат идентификатора")
    })
    public ResponseEntity<TransportRouteDto> getRoute(
            @Parameter(description = "Идентификатор маршрута в OSM", required = true)
            @PathVariable String routeId) {

        TransportRouteDto route = transportService.getRoute(routeId);
        if (route == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(route);
    }
}
