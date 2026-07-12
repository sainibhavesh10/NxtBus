package com.nxtbus.backend.dto;

import com.nxtbus.backend.entity.Trip;

public record TripDto(
        String tripId,
        String routeId,
        String serviceId,
        String shapeId,
        String tripHeadsign,
        Short directionId,
        String blockId
) {

    public static TripDto from(Trip trip) {
        return new TripDto(
                trip.getTripId(),
                trip.getRouteId(),
                trip.getServiceId(),
                trip.getShapeId(),
                trip.getTripHeadsign(),
                trip.getDirectionId(),
                trip.getBlockId()
        );
    }
}