package com.nxtbus.backend.service;

import com.nxtbus.backend.dto.NearbyStopDto;
import com.nxtbus.backend.dto.StopDto;

import java.util.List;

public interface StopService {

    void validateStopExists(String stopId);

    StopDto getStopById(String stopId);

    List<StopDto> searchStopsByName(String query, int limit);

    List<NearbyStopDto> getNearbyStops(
            double lat,
            double lon,
            int limit
    );
}