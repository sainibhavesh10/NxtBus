package com.nxtbus.backend.service;

import com.nxtbus.backend.dto.TripDto;
import org.springframework.data.domain.Page;

public interface TripService {

    void validateTripExists(String tripId);

    TripDto getTripById(String tripId);

    TripDto getRepresentativeTrip(String routeId);

    Page<TripDto> getTripsByRoute(
            String routeId,
            int page,
            int size
    );
}