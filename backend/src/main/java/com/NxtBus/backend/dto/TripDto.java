package com.nxtbus.backend.dto;

import com.nxtbus.backend.entity.Trip;
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

    public Trip toEntity() {
        Trip trip = new Trip();
        trip.setTripId(this.tripId());
        trip.setRouteId(this.routeId());
        trip.setServiceId(this.serviceId());
        trip.setShapeId(this.shapeId());
        trip.setTripHeadsign(this.tripHeadsign());
        trip.setDirectionId(this.directionId());
        trip.setBlockId(this.blockId());
        return trip;
    }
}