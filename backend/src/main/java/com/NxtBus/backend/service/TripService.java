package com.nxtbus.backend.service;

import com.nxtbus.backend.dto.TripDto;
import com.nxtbus.backend.request.PageRequestDto;
import org.springframework.data.domain.Page;

public interface TripService {

    void validateTripExists(String tripId);

    void validateRouteHasTrips(String routeId);

    TripDto getTripById(String tripId);

    Page<TripDto> getTripsByRoute(String routeId, PageRequestDto pageRequest);

    TripDto saveTrip(TripDto tripDto);

    void deleteTrip(String tripId);
}