package ru.oldzoomer.geosearch.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO, представляющее маршрут общественного транспорта.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransportRouteDto {

    /**
     * Уникальный идентификатор маршрута в OSM.
     */
    @NotBlank
    private String id;

    /**
     * Номер или название маршрута.
     */
    @NotBlank
    @Size(max = 50)
    private String name;

    /**
     * Тип маршрута: bus, tram, train, metro, subway, ferry и т.д.
     */
    @NotBlank
    private String type;

    /**
     * Список идентификаторов остановок маршрута.
     */
    private List<String> stops;
}
