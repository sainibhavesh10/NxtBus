package com.nxtbus.backend.dto;

import com.nxtbus.backend.entity.Trip;
import com.nxtbus.backend.repository.projection.TripShapeView;
import com.nxtbus.backend.repository.projection.TripView;

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

    public static TripDto from(TripView view) {
        return new TripDto(
                view.getTripId(),
                view.getRouteId(),
                view.getServiceId(),
                view.getShapeId(),
                view.getTripHeadsign(),
                view.getDirectionId(),
                view.getBlockId()
        );
    }

    public static TripDto from(TripShapeView p) {
        return new TripDto(
                p.getTripId(), p.getRouteId(), p.getServiceId(), p.getShapeId(),
                p.getTripHeadsign(), p.getDirectionId(), p.getBlockId()
        );
    }

}