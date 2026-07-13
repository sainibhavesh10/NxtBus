package com.nxtbus.backend.dto;

import tools.jackson.databind.JsonNode;

public record GeoJsonFeature<T>(
        String type,
        T properties,
        JsonNode geometry
) {
    public static <T> GeoJsonFeature<T> of(T properties, JsonNode geometry) {
        return new GeoJsonFeature<>("Feature", properties, geometry);
    }
}
