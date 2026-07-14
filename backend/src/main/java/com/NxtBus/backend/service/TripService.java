package com.nxtbus.backend.service;

import com.nxtbus.backend.dto.GeoJsonFeature;
import com.nxtbus.backend.dto.TimedStopSequenceDto;
import com.nxtbus.backend.dto.TripDto;
import com.nxtbus.backend.response.RouteStopSequenceResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface TripService {

    void validateTripExists(String tripId);

    TripDto getTripById(String tripId);

    TripDto getRepresentativeTrip(String routeId);

    Page<TripDto> getTripsByRoute(
            String routeId,
            int page,
            int size
    );

    GeoJsonFeature<TripDto> getShapeForTrip(String tripId);
}