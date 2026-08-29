package ru.oldzoomer.geosearch.server.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO, представляющее точку интереса (POI).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoiDto {

    /**
     * Уникальный идентификатор POI в OSM.
     */
    @NotBlank
    private String id;

    /**
     * Название точки интереса.
     */
    @NotBlank
    @Size(max = 255)
    private String name;

    /**
     * Категория POI.
     */
    @NotNull
    private PoiCategory category;

    /**
     * Географическое расположение.
     */
    @NotNull
    @Valid
    private GeoLocation location;

    /**
     * Дополнительные теги OSM (ключ-значение).
     */
    private Map<String, String> tags;

    /**
     * Полный адрес.
     */
    private String address;
}
