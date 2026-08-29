package ru.oldzoomer.geosearch.server.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO, представляющее остановку общественного транспорта.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransportStopDto {

    /**
     * Уникальный идентификатор остановки в OSM.
     */
    @NotBlank
    private String id;

    /**
     * Название остановки.
     */
    @NotBlank
    @Size(max = 255)
    private String name;

    /**
     * Географическое расположение остановки.
     */
    @NotNull
    @Valid
    private GeoLocation location;

    /**
     * Список идентификаторов маршрутов, проходящих через остановку.
     */
    private List<String> routes;

    /**
     * Оператор (перевозчик).
     */
    private String operator;
}
