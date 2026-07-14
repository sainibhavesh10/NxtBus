package com.nxtbus.backend.response;

import com.fasterxml.jackson.annotation.JsonRawValue;
import tools.jackson.databind.JsonNode;

public record GeoJsonFeatureResponse<T>(
        String type,
        T properties,
        @JsonRawValue String geometry
) {
    public static <T> GeoJsonFeatureResponse<T> of(T properties, String geometry) {
        return new GeoJsonFeatureResponse<>("Feature", properties, geometry);
    }
}
