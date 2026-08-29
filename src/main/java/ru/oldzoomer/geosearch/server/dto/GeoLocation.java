package ru.oldzoomer.geosearch.server.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Представляет географическую локацию с координатами широты и долготы.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoLocation {

    /**
     * Широта (latitude) в градусах (-90 до +90).
     */
    @NotNull
    private Double lat;

    /**
     * Долгота (longitude) в градусах (-180 до +180).
     */
    @NotNull
    private Double lon;
}
