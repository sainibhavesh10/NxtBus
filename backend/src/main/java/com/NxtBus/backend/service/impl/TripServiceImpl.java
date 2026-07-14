package com.nxtbus.backend.service.impl;

import com.nxtbus.backend.dto.*;
import com.nxtbus.backend.entity.Trip;
import com.nxtbus.backend.exception.*;
import com.nxtbus.backend.repository.TripRepository;
import com.nxtbus.backend.repository.projection.TripShapeView;
import com.nxtbus.backend.response.RouteStopSequenceResponse;
import com.nxtbus.backend.service.RouteService;
import com.nxtbus.backend.service.TripService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Service
public class TripServiceImpl implements TripService {

    private static final int MAX_PAGE_SIZE = 50;

    private final TripRepository tripRepository;

    private final JsonMapper jsonMapper;


    public TripServiceImpl(TripRepository tripRepository, JsonMapper jsonMapper){
        this.tripRepository = tripRepository;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void validateTripExists(String tripId) {
        if (!tripRepository.existsById(tripId)) {
            throw new RouteNotFoundException(tripId);
        }
    }

    @Override
    public TripDto getTripById(String tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));
        return TripDto.from(trip);
    }

    @Override
    public TripDto getRepresentativeTrip(String routeId) {
        Page<TripDto> trips = getTripsByRoute(routeId, 0, 1); // page 0, size 1 -> first tripId alphabetically
        if (trips.isEmpty()) {
            throw new NoTripsFoundForRouteException(routeId);
        }
        return trips.getContent().getFirst();
    }

    @Override
    public Page<TripDto> getTripsByRoute(String routeId, int page, int size) {
        int cappedSize = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, cappedSize, Sort.by("tripId").ascending());

        return tripRepository.findByRouteId(routeId, pageable).
                map(TripDto::from);
    }

    @Override
    public GeoJsonFeature<TripDto> getShapeForTrip(String tripId) {
        TripShapeView p = tripRepository.findShapeByTripId(tripId)
                .orElseThrow(() -> new ShapeNotFoundException(tripId));

        JsonNode geometry;
        try {
            geometry = jsonMapper.readTree(p.getGeometryJson());
        } catch (JacksonException e) {
            throw new IllegalStateException("Invalid shape geometry for trip " + tripId, e);
        }

        TripDto trip = TripDto.from(p);

        return GeoJsonFeature.of(trip, geometry);
    }
}
