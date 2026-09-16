package com.nxtbus.backend.service;

import com.nxtbus.backend.dto.NearbyStopDto;
import com.nxtbus.backend.dto.StopDto;
import com.nxtbus.backend.request.SearchRequest;
import com.nxtbus.backend.request.StopProximityRequest;
import com.nxtbus.backend.request.SearchRequest;

import java.util.List;

public interface StopService {

    void validateStopExists(String stopId);

    StopDto getStopById(String stopId);

    List<StopDto> searchStopsByName(SearchRequest request);

    List<StopDto> getNearestStops(StopProximityRequest request);

    List<NearbyStopDto> getNearbyStopsWithDistance(StopProximityRequest request);
}