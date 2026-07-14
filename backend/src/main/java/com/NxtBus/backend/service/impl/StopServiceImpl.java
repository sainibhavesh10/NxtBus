package com.nxtbus.backend.service.impl;

import com.nxtbus.backend.entity.Stop;
import com.nxtbus.backend.exception.StopNotFoundException;
import com.nxtbus.backend.repository.StopRepository;
import com.nxtbus.backend.dto.NearbyStopDto;
import com.nxtbus.backend.dto.StopDto;
import com.nxtbus.backend.service.StopService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StopServiceImpl implements StopService {

    private static final int MIN_SEARCH_QUERY_LENGTH = 3;
    private static final int MAX_SEARCH_RESULT_LIMIT = 50;


    private final StopRepository stopRepository;

    public StopServiceImpl(StopRepository stopRepository) {
        this.stopRepository = stopRepository;
    }

    @Override
    public void validateStopExists(String stopId) {
        if (!stopRepository.existsById(stopId)) {
            throw new StopNotFoundException(stopId);
        }
    }

    @Override
    public StopDto getStopById(String stopId) {
        Stop stop = stopRepository.findById(stopId)
                .orElseThrow(() -> new StopNotFoundException(stopId));
        return StopDto.from(stop);
    }

    @Override
    public List<StopDto> searchStopsByName(String query, int limit) {
        if (query == null) {
            return List.of();
        }
        String withoutSpaces = query.replaceAll("\\s+", "");
        //later throw exception here
        if (withoutSpaces.length() < MIN_SEARCH_QUERY_LENGTH) {
            return List.of();
        }
        int safeLimit = Math.max(1, Math.min(limit, MAX_SEARCH_RESULT_LIMIT));
        return stopRepository.searchByName(query.trim(), safeLimit).stream()
                .map(StopDto::from)
                .toList();
    }

    @Override
    public List<NearbyStopDto> getNearbyStops(double lat, double lon, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, MAX_SEARCH_RESULT_LIMIT));
        return stopRepository.findNearestStops(lat, lon, safeLimit * 4, safeLimit)
                .stream()
                .map(NearbyStopDto::from)
                .toList();
    }

}
